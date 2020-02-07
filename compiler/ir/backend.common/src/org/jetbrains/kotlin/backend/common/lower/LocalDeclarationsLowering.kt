/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedFieldDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import java.util.*

interface LocalNameProvider {
    fun localName(declaration: IrDeclarationWithName): String =
        declaration.name.asString()

    companion object {
        val DEFAULT = object : LocalNameProvider {}
    }
}

interface VisibilityPolicy {
    fun forClass(declaration: IrClass, inInlineFunctionScope: Boolean): Visibility =
        declaration.visibility

    fun forConstructor(declaration: IrConstructor, inInlineFunctionScope: Boolean): Visibility =
        Visibilities.PRIVATE

    companion object {
        val DEFAULT = object : VisibilityPolicy {}
    }
}

val IrDeclaration.parentsWithSelf: Sequence<IrDeclarationParent>
    get() = generateSequence(this as? IrDeclarationParent) { (it as? IrDeclaration)?.parent }

val IrDeclaration.parents: Sequence<IrDeclarationParent>
    get() = parentsWithSelf.drop(1)

object BOUND_VALUE_PARAMETER : IrDeclarationOriginImpl("BOUND_VALUE_PARAMETER")

object BOUND_RECEIVER_PARAMETER : IrDeclarationOriginImpl("BOUND_RECEIVER_PARAMETER")

/*
  Local functions raised in LocalDeclarationLowering continue to refer to
  type parameters no longer visible to them.
  We add new type parameters to their declarations, which
  makes JVM accept those declarations. The generated IR is still
  semantically incorrect (TODO: needs further fix), but code generation seems
  to proceed nevertheless.
*/
class LocalDeclarationsLowering(
    val context: BackendContext,
    val localNameProvider: LocalNameProvider = LocalNameProvider.DEFAULT,
    val visibilityPolicy: VisibilityPolicy = VisibilityPolicy.DEFAULT
) :
    BodyLoweringPass {

    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile, allowDeclarationModification = true)
    }

    object DECLARATION_ORIGIN_FIELD_FOR_CAPTURED_VALUE :
        IrDeclarationOriginImpl("FIELD_FOR_CAPTURED_VALUE", isSynthetic = true)

    object DECLARATION_ORIGIN_FIELD_FOR_CROSSINLINE_CAPTURED_VALUE :
        IrDeclarationOriginImpl("FIELD_FOR_CROSSINLINE_CAPTURED_VALUE", isSynthetic = true)

    private object STATEMENT_ORIGIN_INITIALIZER_OF_FIELD_FOR_CAPTURED_VALUE :
        IrStatementOriginImpl("INITIALIZER_OF_FIELD_FOR_CAPTURED_VALUE")

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        LocalDeclarationsTransformer(irBody, container).lowerLocalDeclarations()
    }

    private class ScopeWithCounter(scope: Scope, irElement: IrElement) : ScopeWithIr(scope, irElement) {
        // Continuous numbering across all declarations in the container.
        var counter: Int = 0
    }

    private val scopeMap: MutableMap<IrSymbolOwner, ScopeWithCounter> = mutableMapOf()
    // Need to keep LocalFunctionContext.index
    private val IrSymbolOwner.scopeWithCounter: ScopeWithCounter
        get() = scopeMap.getOrPut(this) {
            ScopeWithCounter(Scope(symbol), this)
        }


    private abstract class LocalContext {
        val capturedTypeParameterToTypeParameter: MutableMap<IrTypeParameter, IrTypeParameter> = mutableMapOf()

        /**
         * @return the expression to get the value for given declaration, or `null` if [IrGetValue] should be used.
         */
        abstract fun irGet(startOffset: Int, endOffset: Int, valueDeclaration: IrValueDeclaration): IrExpression?
    }

    private abstract class LocalContextWithClosureAsParameters : LocalContext() {

        abstract val declaration: IrFunction
        abstract val transformedDeclaration: IrFunction

        val capturedValueToParameter: MutableMap<IrValueDeclaration, IrValueParameter> = mutableMapOf()

        override fun irGet(startOffset: Int, endOffset: Int, valueDeclaration: IrValueDeclaration): IrExpression? {
            val parameter = capturedValueToParameter[valueDeclaration] ?: return null

            return IrGetValueImpl(startOffset, endOffset, parameter.type, parameter.symbol)
        }
    }

    private class LocalFunctionContext(
        override val declaration: IrSimpleFunction,
        val index: Int,
        val ownerForLoweredDeclaration: IrDeclarationContainer
    ) :
        LocalContextWithClosureAsParameters() {
        lateinit var closure: Closure

        override lateinit var transformedDeclaration: IrSimpleFunction
    }

    private class LocalClassConstructorContext(override val declaration: IrConstructor, val inInlineFunctionScope: Boolean) :
        LocalContextWithClosureAsParameters() {
        override lateinit var transformedDeclaration: IrConstructor
    }

    private class LocalClassContext(val declaration: IrClass, val inInlineFunctionScope: Boolean) : LocalContext() {
        lateinit var closure: Closure

        // NOTE: This map is iterated over in `rewriteClassMembers` and we're relying on
        // the deterministic iteration order that `mutableMapOf` provides.
        val capturedValueToField: MutableMap<IrValueDeclaration, IrField> = mutableMapOf()

        override fun irGet(startOffset: Int, endOffset: Int, valueDeclaration: IrValueDeclaration): IrExpression? {
            val field = capturedValueToField[valueDeclaration] ?: return null

            val receiver = declaration.thisReceiver!!
            return IrGetFieldImpl(
                startOffset, endOffset, field.symbol, field.type,
                receiver = IrGetValueImpl(startOffset, endOffset, receiver.type, receiver.symbol)
            )
        }
    }

    private class LocalClassMemberContext(val member: IrFunction, val classContext: LocalClassContext) : LocalContext() {
        override fun irGet(startOffset: Int, endOffset: Int, valueDeclaration: IrValueDeclaration): IrExpression? {
            val field = classContext.capturedValueToField[valueDeclaration] ?: return null

            val receiver = member.dispatchReceiverParameter!!
            return IrGetFieldImpl(
                startOffset, endOffset, field.symbol, field.type,
                receiver = IrGetValueImpl(startOffset, endOffset, receiver.type, receiver.symbol)
            )
        }

    }

    private fun LocalContext.remapType(type: IrType): IrType {
        if (type !is IrSimpleType) return type
        val classifier = (type.classifier as? IrTypeParameterSymbol)?.let { capturedTypeParameterToTypeParameter[it.owner]?.symbol }
            ?: type.classifier
        val arguments = type.arguments.map { remapTypeArgument(it) }
        return IrSimpleTypeImpl(
            classifier, type.hasQuestionMark, arguments, type.annotations,
            type.abbreviation?.let { remapTypeAbbreviation(it) }
        )
    }

    private fun LocalContext.remapTypeArgument(argument: IrTypeArgument) =
        (argument as? IrTypeProjection)?.let { makeTypeProjection(remapType(it.type), it.variance) }
            ?: argument

    private fun LocalContext.remapTypeAbbreviation(abbreviation: IrTypeAbbreviation): IrTypeAbbreviation =
        IrTypeAbbreviationImpl(
            abbreviation.typeAlias,         // TODO: if/when the language gets local or nested type aliases, this will need remapping.
            abbreviation.hasQuestionMark,
            abbreviation.arguments.map { remapTypeArgument(it) },
            abbreviation.annotations
        )

    private inner class LocalDeclarationsTransformer(val irBody: IrBody, val container: IrDeclaration) {
        val localFunctions: MutableMap<IrFunction, LocalFunctionContext> = LinkedHashMap()
        val localClasses: MutableMap<IrClass, LocalClassContext> = LinkedHashMap()
        val localClassConstructors: MutableMap<IrConstructor, LocalClassConstructorContext> = LinkedHashMap()
        val usedLocalFunctionNames: MutableSet<Name> = mutableSetOf()

        val transformedDeclarations = mutableMapOf<IrSymbolOwner, IrDeclaration>()

        val IrFunction.transformed: IrFunction?
            get() = transformedDeclarations[this] as IrFunction?

        val newParameterToOld: MutableMap<IrValueParameter, IrValueParameter> = mutableMapOf()
        val oldParameterToNew: MutableMap<IrValueParameter, IrValueParameter> = mutableMapOf()
        val newParameterToCaptured: MutableMap<IrValueParameter, IrValueSymbol> = mutableMapOf()

        fun lowerLocalDeclarations() {
            collectLocalDeclarations()
            if (localFunctions.isEmpty() && localClasses.isEmpty()) return

            collectClosureForLocalDeclarations()

            transformDeclarations()

            rewriteDeclarations()

            insertLoweredDeclarationForLocalFunctions()
        }

        private fun insertLoweredDeclarationForLocalFunctions() {
            localFunctions.values.forEach {
                it.transformedDeclaration.apply {
                    val original = it.declaration
                    this.body = original.body

                    original.valueParameters.filter { v -> v.defaultValue != null }.forEach { argument ->
                        val body = argument.defaultValue!!
                        oldParameterToNew[argument]!!.defaultValue = body
                    }
                    acceptChildren(SetDeclarationsParentVisitor, this)
                }
                it.ownerForLoweredDeclaration.addChild(it.transformedDeclaration)
            }
        }


        private inner class FunctionBodiesRewriter(val localContext: LocalContext?) : IrElementTransformerVoid() {
            override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) =
                // Both accessors extracted as closures.
                declaration.delegate.transform(this, null)

            override fun visitClass(declaration: IrClass) = if (declaration in localClasses) {
                localClasses[declaration]!!.declaration
            } else {
                super.visitClass(declaration)
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                return if (declaration in localFunctions) {
                    // Replace local function definition with an empty composite.
                    IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.irBuiltIns.unitType)
                } else {
                    if (localContext is LocalClassContext && declaration.parent == localContext.declaration) {
                        declaration.apply {
                            val classMemberLocalContext = LocalClassMemberContext(declaration, localContext)
                            transformChildrenVoid(FunctionBodiesRewriter(classMemberLocalContext))
                        }
                    } else {
                        super.visitFunction(declaration)
                    }
                }
            }

            override fun visitConstructor(declaration: IrConstructor): IrStatement {
                // Body is transformed separately. See loop over constructors in rewriteDeclarations().

                val constructorContext = localClassConstructors[declaration]
                return constructorContext?.transformedDeclaration?.apply {
                    this.body = declaration.body!!

                    declaration.valueParameters.filter { it.defaultValue != null }.forEach { argument ->
                        oldParameterToNew[argument]!!.defaultValue = argument.defaultValue
                    }
                    acceptChildren(SetDeclarationsParentVisitor, this)
                } ?: super.visitConstructor(declaration)
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val declaration = expression.symbol.owner

                localContext?.irGet(expression.startOffset, expression.endOffset, declaration)?.let {
                    return it
                }

                oldParameterToNew[declaration]?.let {
                    return IrGetValueImpl(expression.startOffset, expression.endOffset, it.type, it.symbol)
                }

                return expression
            }

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                val oldCallee = expression.symbol.owner
                val newCallee = (oldCallee.transformed ?: return expression) as IrSimpleFunction

                return createNewCall(expression, newCallee).fillArguments2(expression, newCallee)
            }

            override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                val oldCallee = expression.symbol.owner
                val newCallee = (oldCallee.transformed ?: return expression) as IrConstructor

                return createNewCall(expression, newCallee).fillArguments2(expression, newCallee)
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                val oldCallee = expression.symbol.owner
                val newCallee = transformedDeclarations[oldCallee] as IrConstructor? ?: return expression

                return IrDelegatingConstructorCallImpl(
                    expression.startOffset, expression.endOffset,
                    context.irBuiltIns.unitType,
                    newCallee.symbol,
                    expression.typeArgumentsCount
                ).also {
                    it.fillArguments2(expression, newCallee)
                    it.copyTypeArgumentsFrom(expression)
                }
            }

            inline fun <T : IrMemberAccessExpression> T.mapValueParameters(
                newTarget: IrFunction,
                transform: (IrValueParameter) -> IrExpression?
            ): T =
                apply {
                    for (p in newTarget.valueParameters) {
                        putValueArgument(p.index, transform(p))
                    }
                }

            private fun <T : IrMemberAccessExpression> T.fillArguments2(oldExpression: IrMemberAccessExpression, newTarget: IrFunction): T {

                mapValueParameters(newTarget) { newValueParameterDeclaration ->
                    val oldParameter = newParameterToOld[newValueParameterDeclaration]

                    if (oldParameter != null) {
                        oldExpression.getValueArgument(oldParameter.index)
                    } else {
                        // The callee expects captured value as argument.
                        val capturedValueSymbol =
                            newParameterToCaptured[newValueParameterDeclaration]
                                ?: throw AssertionError("Non-mapped parameter $newValueParameterDeclaration")

                        val capturedValue = capturedValueSymbol.owner

                        localContext?.irGet(oldExpression.startOffset, oldExpression.endOffset, capturedValue) ?: run {
                            // Captured value is directly available for the caller.
                            val value = oldParameterToNew[capturedValue] ?: capturedValue
                            IrGetValueImpl(oldExpression.startOffset, oldExpression.endOffset, value.symbol)
                        }
                    }

                }

                dispatchReceiver = oldExpression.dispatchReceiver
                extensionReceiver = oldExpression.extensionReceiver

                return this
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                expression.transformChildrenVoid(this)

                val oldCallee = expression.symbol.owner
                val newCallee = oldCallee.transformed ?: return expression
                val newReflectionTarget = expression.reflectionTarget?.run { owner.transformed }

                return IrFunctionReferenceImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type, // TODO functional type for transformed descriptor
                    newCallee.symbol,
                    newCallee.typeParameters.size,
                    newReflectionTarget?.symbol,
                    expression.origin
                ).also {
                    it.fillArguments2(expression, newCallee)
                    it.setLocalTypeArguments(oldCallee)
                    it.copyTypeArgumentsFrom(expression, shift = newCallee.typeParameters.size - expression.typeArgumentsCount)
                    it.copyAttributes(expression)
                }
            }

            override fun visitReturn(expression: IrReturn): IrExpression {
                expression.transformChildrenVoid(this)

                val oldReturnTarget = expression.returnTargetSymbol.owner as? IrFunction ?: return expression
                val newReturnTarget = oldReturnTarget.transformed ?: return expression

                return IrReturnImpl(
                    expression.startOffset, expression.endOffset,
                    context.irBuiltIns.nothingType,
                    newReturnTarget.symbol, expression.value
                )
            }

            override fun visitDeclarationReference(expression: IrDeclarationReference): IrExpression {
                if (expression.symbol.owner in transformedDeclarations) {
                    TODO()
                }
                return super.visitDeclarationReference(expression)
            }

            override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                if (declaration is IrSymbolOwner && declaration in transformedDeclarations) {
                    TODO()
                }
                return super.visitDeclaration(declaration)
            }
        }

        private fun rewriteFunctionBody(irDeclaration: IrElement, localContext: LocalContext?) {
            irDeclaration.transformChildrenVoid(FunctionBodiesRewriter(localContext))
        }

        private fun rewriteClassMembers(irClass: IrClass, localClassContext: LocalClassContext) {
            val constructors = irClass.declarations.filterIsInstance<IrConstructor>()

            irClass.transformChildrenVoid(FunctionBodiesRewriter(localClassContext))

            val constructorsCallingSuper = constructors
                .asSequence()
                .map { localClassConstructors[it]!! }
                .filter { it.declaration.callsSuper(context.irBuiltIns) }
                .toList()

            assert(constructorsCallingSuper.any()) { "Expected at least one constructor calling super; class: $irClass" }

            irClass.declarations += localClassContext.capturedValueToField.values

            for (constructorContext in constructorsCallingSuper) {
                val blockBody = constructorContext.declaration.body as? IrBlockBody
                    ?: throw AssertionError("Unexpected constructor body: ${constructorContext.declaration.body}")

                // NOTE: It's important to set the fields for captured values in the same order as the arguments,
                // since `AnonymousObjectTransformer` relies on this ordering.
                blockBody.statements.addAll(
                    0,
                    localClassContext.capturedValueToField.map { (capturedValue, field) ->
                        IrSetFieldImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET, field.symbol,
                            IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irClass.thisReceiver!!.symbol),
                            constructorContext.irGet(UNDEFINED_OFFSET, UNDEFINED_OFFSET, capturedValue)!!,
                            context.irBuiltIns.unitType,
                            STATEMENT_ORIGIN_INITIALIZER_OF_FIELD_FOR_CAPTURED_VALUE
                        )
                    }
                )
            }
        }

        private fun rewriteDeclarations() {
            localFunctions.values.forEach {
                rewriteFunctionBody(it.declaration, it)
            }

            localClassConstructors.values.forEach {
                rewriteFunctionBody(it.declaration, it)
            }

            localClasses.values.forEach {
                rewriteClassMembers(it.declaration, it)
            }

            rewriteFunctionBody(container, null)
        }

        private fun createNewCall(oldCall: IrCall, newCallee: IrFunction) =
            IrCallImpl(
                oldCall.startOffset, oldCall.endOffset,
                newCallee.returnType,
                newCallee.symbol,
                newCallee.typeParameters.size,
                oldCall.origin,
                oldCall.superQualifierSymbol
            ).also {
                it.setLocalTypeArguments(oldCall.symbol.owner)
                it.copyTypeArgumentsFrom(oldCall, shift = newCallee.typeParameters.size - oldCall.typeArgumentsCount)
            }

        private fun createNewCall(oldCall: IrConstructorCall, newCallee: IrConstructor) =
            IrConstructorCallImpl.fromSymbolOwner(
                oldCall.startOffset, oldCall.endOffset,
                newCallee.returnType,
                newCallee.symbol,
                newCallee.parentAsClass.typeParameters.size,
                oldCall.origin
            ).also {
                it.copyTypeArgumentsFrom(oldCall)
            }

        private fun IrMemberAccessExpression.setLocalTypeArguments(callee: IrFunction) {
            val context = localFunctions[callee] ?: return
            for ((outerTypeParameter, innerTypeParameter) in context.capturedTypeParameterToTypeParameter) {
                putTypeArgument(innerTypeParameter.index, outerTypeParameter.defaultType) // TODO: remap default type!
            }
        }

        private fun transformDeclarations() {
            localFunctions.values.forEach {
                createLiftedDeclaration(it)
            }

            localClasses.values.forEach {
                val localClassVisibility = visibilityPolicy.forClass(it.declaration, it.inInlineFunctionScope)
                it.declaration.visibility = localClassVisibility
                createFieldsForCapturedValues(it)
            }

            localClassConstructors.values.forEach {
                createTransformedConstructorDeclaration(it)
            }
        }

        private fun suggestLocalName(declaration: IrDeclarationWithName): String {
            localFunctions[declaration]?.let {
                val baseName = if (declaration.name.isSpecial) "lambda" else declaration.name
                if (it.index >= 0)
                    return "$baseName-${it.index}"
            }

            return localNameProvider.localName(declaration)
        }

        private fun generateNameForLiftedDeclaration(
            declaration: IrDeclaration,
            newOwner: IrDeclarationParent
        ): Name =
            Name.identifier(
                declaration.parentsWithSelf
                    .takeWhile { it != newOwner }
                    .toList().reversed().joinToString(separator = "$") { suggestLocalName(it as IrDeclarationWithName) }
            )

        private fun createLiftedDeclaration(localFunctionContext: LocalFunctionContext) {
            val oldDeclaration = localFunctionContext.declaration
            assert(oldDeclaration.dispatchReceiverParameter == null)

            val memberOwner = localFunctionContext.ownerForLoweredDeclaration
            val newDescriptor = WrappedSimpleFunctionDescriptor(oldDeclaration.descriptor)
            val newSymbol = IrSimpleFunctionSymbolImpl(newDescriptor)
            val newName = generateNameForLiftedDeclaration(oldDeclaration, memberOwner)

            if (oldDeclaration.dispatchReceiverParameter != null) {
                throw AssertionError("local functions must not have dispatch receiver")
            }

            // TODO: consider using fields to access the closure of enclosing class.
            val (capturedValues, capturedTypeParameters) = localFunctionContext.closure

            val newDeclaration = IrFunctionImpl(
                oldDeclaration.startOffset,
                oldDeclaration.endOffset,
                oldDeclaration.origin,
                newSymbol,
                newName,
                Visibilities.PRIVATE,
                Modality.FINAL,
                returnType = IrUninitializedType,
                isInline = oldDeclaration.isInline,
                isExternal = oldDeclaration.isExternal,
                isTailrec = oldDeclaration.isTailrec,
                isSuspend = oldDeclaration.isSuspend,
                isExpect = oldDeclaration.isExpect,
                isFakeOverride = oldDeclaration.isFakeOverride,
                isOperator = oldDeclaration.isOperator
            )
            newDescriptor.bind(newDeclaration)

            localFunctionContext.transformedDeclaration = newDeclaration

            val newTypeParameters = newDeclaration.copyTypeParameters(capturedTypeParameters)
            localFunctionContext.capturedTypeParameterToTypeParameter.putAll(
                capturedTypeParameters.zip(newTypeParameters)
            )
            newDeclaration.copyTypeParametersFrom(oldDeclaration, parameterMap = localFunctionContext.capturedTypeParameterToTypeParameter)
            // Type parameters of oldDeclaration may depend on captured type parameters, so deal with that after copying.
            newDeclaration.typeParameters.drop(newTypeParameters.size).forEach { tp ->
                tp.superTypes.replaceAll { localFunctionContext.remapType(it) }
            }

            newDeclaration.parent = memberOwner
            newDeclaration.returnType = localFunctionContext.remapType(oldDeclaration.returnType)
            newDeclaration.dispatchReceiverParameter = null
            newDeclaration.extensionReceiverParameter = oldDeclaration.extensionReceiverParameter?.run {
                copyTo(newDeclaration, type = localFunctionContext.remapType(this.type)).also {
                    newParameterToOld.putAbsentOrSame(it, this)
                }
            }
            newDeclaration.copyAttributes(oldDeclaration)

            newDeclaration.valueParameters += createTransformedValueParameters(
                capturedValues, localFunctionContext, oldDeclaration, newDeclaration
            )
            newDeclaration.recordTransformedValueParameters(localFunctionContext)

            newDeclaration.annotations = oldDeclaration.annotations

            transformedDeclarations[oldDeclaration] = newDeclaration
        }

        private fun createTransformedValueParameters(
            capturedValues: List<IrValueSymbol>,
            localFunctionContext: LocalContext,
            oldDeclaration: IrFunction,
            newDeclaration: IrFunction
        ) = ArrayList<IrValueParameter>(capturedValues.size + oldDeclaration.valueParameters.size).apply {
            val generatedNames = mutableSetOf<Name>()
            capturedValues.mapIndexedTo(this) { i, capturedValue ->
                val parameterDescriptor = WrappedValueParameterDescriptor()
                val p = capturedValue.owner
                IrValueParameterImpl(
                    p.startOffset,
                    p.endOffset,
                    if (p.descriptor is ReceiverParameterDescriptor && newDeclaration is IrConstructor)
                        BOUND_RECEIVER_PARAMETER else BOUND_VALUE_PARAMETER,
                    IrValueParameterSymbolImpl(parameterDescriptor),
                    suggestNameForCapturedValue(p, generatedNames),
                    i,
                    localFunctionContext.remapType(p.type),
                    null,
                    isCrossinline = (capturedValue as? IrValueParameterSymbol)?.owner?.isCrossinline == true,
                    isNoinline = false
                ).also {
                    parameterDescriptor.bind(it)
                    it.parent = newDeclaration
                    newParameterToCaptured[it] = capturedValue
                }
            }

            oldDeclaration.valueParameters.mapTo(this) { v ->
                v.copyTo(
                    newDeclaration,
                    index = v.index + capturedValues.size,
                    type = localFunctionContext.remapType(v.type)
                ).also {
                    newParameterToOld.putAbsentOrSame(it, v)
                }
            }
        }

        private fun IrFunction.recordTransformedValueParameters(localContext: LocalContextWithClosureAsParameters) {

            valueParameters.forEach {
                val capturedValue = newParameterToCaptured[it]
                if (capturedValue != null) {
                    localContext.capturedValueToParameter[capturedValue.owner] = it
                }
            }

            (listOfNotNull(dispatchReceiverParameter, extensionReceiverParameter) + valueParameters).forEach {
                val oldParameter = newParameterToOld[it]
                if (oldParameter != null) {
                    oldParameterToNew.putAbsentOrSame(oldParameter, it)
                }
            }

        }

        private fun createTransformedConstructorDeclaration(constructorContext: LocalClassConstructorContext) {
            val oldDeclaration = constructorContext.declaration

            val localClassContext = localClasses[oldDeclaration.parent]!!
            val capturedValues = localClassContext.closure.capturedValues

            val newDescriptor = WrappedClassConstructorDescriptor(oldDeclaration.descriptor.annotations, oldDeclaration.descriptor.source)
            val newSymbol = IrConstructorSymbolImpl(newDescriptor)

            val loweredConstructorVisibility =
                visibilityPolicy.forConstructor(oldDeclaration, constructorContext.inInlineFunctionScope)

            val newDeclaration = IrConstructorImpl(
                oldDeclaration.startOffset, oldDeclaration.endOffset, oldDeclaration.origin,
                newSymbol, oldDeclaration.name, loweredConstructorVisibility, oldDeclaration.returnType,
                isInline = oldDeclaration.isInline,
                isExternal = oldDeclaration.isExternal,
                isPrimary = oldDeclaration.isPrimary,
                isExpect = oldDeclaration.isExpect
            )

            newDescriptor.bind(newDeclaration)

            constructorContext.transformedDeclaration = newDeclaration

            newDeclaration.parent = localClassContext.declaration
            newDeclaration.copyTypeParametersFrom(oldDeclaration)

            oldDeclaration.dispatchReceiverParameter?.run {
                throw AssertionError("Local class constructor can't have dispatch receiver: ${ir2string(oldDeclaration)}")
            }
            oldDeclaration.extensionReceiverParameter?.run {
                throw AssertionError("Local class constructor can't have extension receiver: ${ir2string(oldDeclaration)}")
            }

            newDeclaration.valueParameters += createTransformedValueParameters(capturedValues, localClassContext, oldDeclaration, newDeclaration)
            newDeclaration.recordTransformedValueParameters(constructorContext)

            newDeclaration.metadata = oldDeclaration.metadata

            transformedDeclarations[oldDeclaration] = newDeclaration
        }

        private fun createFieldForCapturedValue(
            startOffset: Int,
            endOffset: Int,
            name: Name,
            visibility: Visibility,
            parent: IrClass,
            fieldType: IrType,
            isCrossinline: Boolean
        ): IrField {
            val descriptor = WrappedFieldDescriptor()
            val symbol = IrFieldSymbolImpl(descriptor)
            return IrFieldImpl(
                startOffset,
                endOffset,
                if (isCrossinline) DECLARATION_ORIGIN_FIELD_FOR_CROSSINLINE_CAPTURED_VALUE else DECLARATION_ORIGIN_FIELD_FOR_CAPTURED_VALUE,
                symbol,
                name,
                fieldType,
                visibility,
                isFinal = true,
                isExternal = false,
                isStatic = false,
                isFakeOverride = false
            ).also {
                descriptor.bind(it)
                it.parent = parent
            }
        }

        private fun createFieldsForCapturedValues(localClassContext: LocalClassContext) {
            val classDeclaration = localClassContext.declaration
            val generatedNames = mutableSetOf<Name>()
            localClassContext.closure.capturedValues.forEach { capturedValue ->

                val owner = capturedValue.owner
                val irField = createFieldForCapturedValue(
                    classDeclaration.startOffset,
                    classDeclaration.endOffset,
                    suggestNameForCapturedValue(owner, generatedNames),
                    Visibilities.PRIVATE,
                    classDeclaration,
                    owner.type,
                    owner is IrValueParameter && owner.isCrossinline
                )

                localClassContext.capturedValueToField[owner] = irField
            }
        }

        private fun <K, V> MutableMap<K, V>.putAbsentOrSame(key: K, value: V) {
            val current = this.getOrPut(key) { value }

            if (current != value) {
                error("$current != $value")
            }
        }

        private fun Name.stripSpecialMarkers(): String =
            if (isSpecial) asString().substring(1, asString().length - 1) else asString()

        private fun suggestNameForCapturedValue(declaration: IrValueDeclaration, existing: MutableSet<Name>): Name {
            val base = if (declaration.name.isSpecial)
                declaration.name.stripSpecialMarkers()
            else
                declaration.name.asString()
            var chosen = base.synthesizedName
            var suffix = 0
            while (!existing.add(chosen))
                chosen = "$base$${++suffix}".synthesizedName
            return chosen
        }


        private fun collectClosureForLocalDeclarations() {
            //TODO: maybe use for granular declarations
            val annotator = ClosureAnnotator(irBody, container)

            localFunctions.forEach { (declaration, context) ->
                context.closure = annotator.getFunctionClosure(declaration)
            }

            localClasses.forEach { (declaration, context) ->
                context.closure = annotator.getClassClosure(declaration)
            }
        }

        private fun collectLocalDeclarations() {
            val enclosingFileScope = container.file.scopeWithCounter

            val enclosingClassScope = run {
                var currentParent = container as? IrClass ?: container.parent
                while (currentParent is IrDeclaration && currentParent !is IrClass) {
                    currentParent = currentParent.parent
                }

                currentParent as? IrClass
            }?.scopeWithCounter

            irBody.acceptVoid(object : IrElementVisitorVoidWithContext() {

                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun createScope(declaration: IrSymbolOwner): ScopeWithIr {
                    return ScopeWithCounter(Scope(declaration.symbol), declaration) // Don't cache local declarations
                }

                override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                    super.visitSimpleFunction(declaration)

                    if (declaration.visibility == Visibilities.LOCAL) {
                        val scopeWithIr =
                            (currentClass ?: enclosingClassScope ?: enclosingFileScope /*file is required for K/N cause file declarations are not split by classes*/
                            ?: error("No scope for ${declaration.dump()}"))
                        localFunctions[declaration] =
                            LocalFunctionContext(
                                declaration,
                                if (declaration.name.isSpecial || declaration.name in usedLocalFunctionNames)
                                    (scopeWithIr as ScopeWithCounter).counter++
                                else -1,
                                scopeWithIr.irElement as IrDeclarationContainer
                            )
                        usedLocalFunctionNames.add(declaration.name)
                    }
                }

                override fun visitConstructor(declaration: IrConstructor) {
                    super.visitConstructor(declaration)

                    if (!declaration.constructedClass.isLocalNotInner()) return

                    localClassConstructors[declaration] = LocalClassConstructorContext(declaration, inInlineFunctionScope)
                }

                override fun visitClassNew(declaration: IrClass) {
                    super.visitClassNew(declaration)

                    if (!declaration.isLocalNotInner()) return

                    val localClassContext = LocalClassContext(declaration, inInlineFunctionScope)
                    localClasses[declaration] = localClassContext
                }

                private val inInlineFunctionScope: Boolean
                    get() = allScopes.any { scope -> (scope.irElement as? IrFunction)?.isInline ?: false } ||
                            generateSequence(container) { it.parent as? IrDeclaration }.any { it is IrFunction && it.isInline }
            })
        }
    }

}

// Local inner classes capture anything through outer
internal fun IrClass.isLocalNotInner(): Boolean = visibility == Visibilities.LOCAL && !isInner