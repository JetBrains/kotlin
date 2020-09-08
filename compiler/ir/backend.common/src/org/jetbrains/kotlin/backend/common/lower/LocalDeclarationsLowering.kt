/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

interface LocalNameProvider {
    fun localName(declaration: IrDeclarationWithName): String =
        declaration.name.asString()

    companion object {
        val DEFAULT = object : LocalNameProvider {}
    }
}

interface VisibilityPolicy {
    fun forClass(declaration: IrClass, inInlineFunctionScope: Boolean): DescriptorVisibility =
        declaration.visibility

    fun forConstructor(declaration: IrConstructor, inInlineFunctionScope: Boolean): DescriptorVisibility =
        DescriptorVisibilities.PRIVATE

    fun forCapturedField(value: IrValueSymbol): DescriptorVisibility =
        DescriptorVisibilities.PRIVATE

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
    val context: CommonBackendContext,
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
        LocalDeclarationsTransformer(irBody, container, null).lowerLocalDeclarations()
    }

    fun lower(irElement: IrElement, container: IrDeclaration, classesToLower: Set<IrClass>) {
        LocalDeclarationsTransformer(irElement, container, classesToLower).lowerLocalDeclarations()
    }

    internal class ScopeWithCounter(val irElement: IrElement) {
        // Continuous numbering across all declarations in the container.
        var counter: Int = 0
        val usedLocalFunctionNames: MutableSet<Name> = hashSetOf()
    }

    internal class LocalScopeWithCounterMap {
        val scopeMap: MutableMap<IrSymbolOwner, ScopeWithCounter> = hashMapOf()
    }

    // Need to keep LocalFunctionContext.index
    private val IrSymbolOwner.scopeWithCounter: ScopeWithCounter
        get() = context.ir.localScopeWithCounterMap.scopeMap.getOrPut(this) {
            ScopeWithCounter(this)
        }

    private abstract class LocalContext {
        val capturedTypeParameterToTypeParameter: MutableMap<IrTypeParameter, IrTypeParameter> = mutableMapOf()
        // By the time typeRemapper is used, the map will be already filled
        val typeRemapper = IrTypeParameterRemapper(capturedTypeParameterToTypeParameter)

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

    private fun LocalContext.remapType(type: IrType): IrType = typeRemapper.remapType(type)

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private inner class LocalDeclarationsTransformer(
        val irElement: IrElement, val container: IrDeclaration, val classesToLower: Set<IrClass>?
    ) {
        val localFunctions: MutableMap<IrFunction, LocalFunctionContext> = LinkedHashMap()
        val localClasses: MutableMap<IrClass, LocalClassContext> = LinkedHashMap()
        val localClassConstructors: MutableMap<IrConstructor, LocalClassConstructorContext> = LinkedHashMap()

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
                    this.body?.remapTypes(it.typeRemapper)

                    original.valueParameters.filter { v -> v.defaultValue != null }.forEach { argument ->
                        val body = argument.defaultValue!!
                        body.remapTypes(it.typeRemapper)
                        oldParameterToNew[argument]!!.defaultValue = body
                    }
                    acceptChildren(SetDeclarationsParentVisitor, this)
                }
                it.ownerForLoweredDeclaration.addChild(it.transformedDeclaration)
            }
        }


        private inner class FunctionBodiesRewriter(val localContext: LocalContext?) : IrElementTransformerVoid() {
            override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement =
                // Both accessors extracted as closures.
                declaration.delegate.transformStatement(this)

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
                    typeArgumentsCount = expression.typeArgumentsCount,
                    valueArgumentsCount = newCallee.valueParameters.size
                ).also {
                    it.fillArguments2(expression, newCallee)
                    it.copyTypeArgumentsFrom(expression)
                }
            }

            inline fun <T : IrMemberAccessExpression<*>> T.mapValueParameters(
                newTarget: IrFunction,
                transform: (IrValueParameter) -> IrExpression?
            ): T =
                apply {
                    for (p in newTarget.valueParameters) {
                        putValueArgument(p.index, transform(p))
                    }
                }

            private fun <T : IrMemberAccessExpression<*>> T.fillArguments2(
                oldExpression: IrMemberAccessExpression<*>,
                newTarget: IrFunction
            ): T {
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
                    typeArgumentsCount = newCallee.typeParameters.size,
                    valueArgumentsCount = newCallee.valueParameters.size,
                    reflectionTarget = newReflectionTarget?.symbol,
                    origin = expression.origin
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

            override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
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

            context.mapping.capturedFields[irClass] =
                (context.mapping.capturedFields[irClass] ?: emptyList()) + localClassContext.capturedValueToField.values

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

            rewriteFunctionBody(irElement, null)
        }

        private fun createNewCall(oldCall: IrCall, newCallee: IrSimpleFunction) =
            IrCallImpl(
                oldCall.startOffset, oldCall.endOffset,
                oldCall.type,
                newCallee.symbol,
                typeArgumentsCount = newCallee.typeParameters.size,
                valueArgumentsCount = newCallee.valueParameters.size,
                origin = oldCall.origin,
                superQualifierSymbol = oldCall.superQualifierSymbol
            ).also {
                it.setLocalTypeArguments(oldCall.symbol.owner)
                it.copyTypeArgumentsFrom(oldCall, shift = newCallee.typeParameters.size - oldCall.typeArgumentsCount)
            }

        private fun createNewCall(oldCall: IrConstructorCall, newCallee: IrConstructor) =
            IrConstructorCallImpl.fromSymbolOwner(
                oldCall.startOffset, oldCall.endOffset,
                oldCall.type,
                newCallee.symbol,
                newCallee.parentAsClass.typeParameters.size,
                oldCall.origin
            ).also {
                it.copyTypeArgumentsFrom(oldCall)
            }

        private fun IrMemberAccessExpression<*>.setLocalTypeArguments(callee: IrFunction) {
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
        ): Name {
            val parents = declaration.parentsWithSelf.takeWhile { it != newOwner }.toList().reversed()
            val nameFromParents = parents.joinToString(separator = "$") { suggestLocalName(it as IrDeclarationWithName) }
            // Local functions declared in anonymous initializers have classes as their parents.
            // Such anonymous initializers, however, are inlined into the constructors delegating to super class constructor.
            // There can be local functions declared in local function in init blocks (and further),
            // but such functions would have proper "safe" names (outerLocalFun1$outerLocalFun2$...$localFun).
            return if (parents.size == 1 && declaration.parent is IrClass)
                Name.identifier("_init_\$$nameFromParents")
            else
                Name.identifier(nameFromParents)
        }

        private fun createLiftedDeclaration(localFunctionContext: LocalFunctionContext) {
            val oldDeclaration = localFunctionContext.declaration
            if (oldDeclaration.dispatchReceiverParameter != null) {
                throw AssertionError("local functions must not have dispatch receiver")
            }

            val memberOwner = localFunctionContext.ownerForLoweredDeclaration
            val newName = generateNameForLiftedDeclaration(oldDeclaration, memberOwner)

            // TODO: consider using fields to access the closure of enclosing class.
            val (capturedValues, capturedTypeParameters) = localFunctionContext.closure

            val newDeclaration = context.irFactory.buildFun {
                updateFrom(oldDeclaration)
                name = newName
                visibility = DescriptorVisibilities.PRIVATE
                modality = Modality.FINAL
            }

            localFunctionContext.transformedDeclaration = newDeclaration

            val newTypeParameters = newDeclaration.copyTypeParameters(capturedTypeParameters)
            localFunctionContext.capturedTypeParameterToTypeParameter.putAll(
                capturedTypeParameters.zip(newTypeParameters)
            )
            newDeclaration.copyTypeParametersFrom(oldDeclaration, parameterMap = localFunctionContext.capturedTypeParameterToTypeParameter)
            localFunctionContext.capturedTypeParameterToTypeParameter.putAll(
                oldDeclaration.typeParameters.zip(newDeclaration.typeParameters.drop(newTypeParameters.size))
            )
            // Type parameters of oldDeclaration may depend on captured type parameters, so deal with that after copying.
            newDeclaration.typeParameters.drop(newTypeParameters.size).forEach { tp ->
                tp.superTypes = tp.superTypes.map { localFunctionContext.remapType(it) }
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
            val generatedNames = mutableSetOf<String>()
            capturedValues.mapIndexedTo(this) { i, capturedValue ->
                val p = capturedValue.owner
                buildValueParameter(newDeclaration) {
                    startOffset = p.startOffset
                    endOffset = p.endOffset
                    origin =
                        if (p is IrValueParameter && p.index < 0 && newDeclaration is IrConstructor) BOUND_RECEIVER_PARAMETER
                        else BOUND_VALUE_PARAMETER
                    name = suggestNameForCapturedValue(p, generatedNames)
                    index = i
                    type = localFunctionContext.remapType(p.type)
                    isCrossInline = (capturedValue as? IrValueParameterSymbol)?.owner?.isCrossinline == true
                    isNoinline = (capturedValue as? IrValueParameterSymbol)?.owner?.isNoinline == true
                }.also {
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

            val newDeclaration = context.irFactory.buildConstructor {
                updateFrom(oldDeclaration)
                visibility = visibilityPolicy.forConstructor(oldDeclaration, constructorContext.inInlineFunctionScope)
                returnType = oldDeclaration.returnType
            }

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
            context.mapping.capturedConstructors[oldDeclaration] = newDeclaration
        }

        private fun createFieldForCapturedValue(
                startOffset: Int,
                endOffset: Int,
                name: Name,
                visibility: DescriptorVisibility,
                parent: IrClass,
                fieldType: IrType,
                isCrossinline: Boolean
        ): IrField =
            context.irFactory.buildField {
                this.startOffset = startOffset
                this.endOffset = endOffset
                this.origin =
                    if (isCrossinline) DECLARATION_ORIGIN_FIELD_FOR_CROSSINLINE_CAPTURED_VALUE
                    else DECLARATION_ORIGIN_FIELD_FOR_CAPTURED_VALUE
                this.name = name
                this.type = fieldType
                this.visibility = visibility
                this.isFinal = true
            }.also {
                it.parent = parent
            }

        private fun createFieldsForCapturedValues(localClassContext: LocalClassContext) {
            val classDeclaration = localClassContext.declaration
            val generatedNames = mutableSetOf<String>()
            localClassContext.closure.capturedValues.forEach { capturedValue ->

                val owner = capturedValue.owner
                val irField = createFieldForCapturedValue(
                    classDeclaration.startOffset,
                    classDeclaration.endOffset,
                    suggestNameForCapturedValue(owner, generatedNames),
                    visibilityPolicy.forCapturedField(capturedValue),
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

        private fun suggestNameForCapturedValue(declaration: IrValueDeclaration, usedNames: MutableSet<String>): Name {
            if (declaration is IrValueParameter && declaration.name.asString() == "<this>") {
                if (declaration.isDispatchReceiver()) {
                    return findFirstUnusedName("this\$0", usedNames) {
                        "this\$$it"
                    }
                } else if (declaration.isExtensionReceiver()) {
                    val parentNameSuffix = declaration.parentNameSuffixForExtensionReceiver
                    return findFirstUnusedName("\$this_$parentNameSuffix", usedNames) {
                        "\$this_$parentNameSuffix\$$it"
                    }
                }
                // TODO captured extension receivers of extension lambdas?
            }
            val base = if (declaration.name.isSpecial)
                declaration.name.stripSpecialMarkers()
            else
                declaration.name.asString()
            return findFirstUnusedName(base.synthesizedString, usedNames) {
                "$base$$it".synthesizedString
            }
        }

        private inline fun findFirstUnusedName(initialName: String, usedNames: MutableSet<String>, nextName: (Int) -> String): Name {
            var chosen = initialName
            var suffix = 0
            while (!usedNames.add(chosen))
                chosen = nextName(++suffix)
            return Name.identifier(chosen)
        }

        private fun IrValueParameter.isDispatchReceiver(): Boolean =
            when (val parent = this.parent) {
                is IrFunction ->
                    parent.dispatchReceiverParameter == this
                is IrClass ->
                    parent.thisReceiver == this
                else ->
                    false
            }

        private fun IrValueParameter.isExtensionReceiver(): Boolean {
            val parentFun = parent as? IrFunction ?: return false
            return parentFun.extensionReceiverParameter == this
        }

        private val IrValueParameter.parentNameSuffixForExtensionReceiver: String
            get() {
                val parentFun = parent as? IrSimpleFunction
                    ?: throw AssertionError("Extension receiver parent is not a simple function: ${parent.render()}")
                val correspondingProperty = parentFun.safeAs<IrSimpleFunction>()?.correspondingPropertySymbol?.owner
                return when {
                    correspondingProperty != null ->
                        correspondingProperty.name.stripSpecialMarkers()
                    else ->
                        parentFun.name.stripSpecialMarkers()
                }
            }

        private fun collectClosureForLocalDeclarations() {
            //TODO: maybe use for granular declarations
            val annotator = ClosureAnnotator(irElement, container)

            localFunctions.forEach { (declaration, context) ->
                context.closure = annotator.getFunctionClosure(declaration)
            }

            localClasses.forEach { (declaration, context) ->
                context.closure = annotator.getClassClosure(declaration)
            }
        }

        private fun collectLocalDeclarations() {
            val enclosingFile = container.file
            val enclosingClass = run {
                var currentParent = container as? IrClass ?: container.parent
                while (currentParent is IrDeclaration && currentParent !is IrClass) {
                    currentParent = currentParent.parent
                }

                currentParent as? IrClass
            }

            class Data(val currentClass: ScopeWithCounter?, val isInInlineFunction: Boolean) {
                fun withCurrentClass(currentClass: IrClass): Data =
                    // Don't cache local declarations
                    Data(ScopeWithCounter(currentClass), isInInlineFunction)

                fun withInline(isInline: Boolean): Data =
                    if (isInline && !isInInlineFunction) Data(currentClass, true) else this
            }

            irElement.accept(object : IrElementVisitor<Unit, Data> {
                override fun visitElement(element: IrElement, data: Data) {
                    element.acceptChildren(this, data)
                }

                override fun visitFunctionExpression(expression: IrFunctionExpression, data: Data) {
                    // TODO: For now IrFunctionExpression can only be encountered here if this was called from the inliner,
                    // then all IrFunctionExpression will be replaced by IrFunctionReferenceExpression.
                    // Don't forget to fix this when that replacement has been dropped.
                    // Also, a note: even if a lambda is not an inline one, there still cannot be a reference to it
                    // from an outside declaration, so it is safe to skip them here and correctly handle later, after the above conversion.
                    expression.function.acceptChildren(this, data)
                }

                override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Data) {
                    super.visitSimpleFunction(declaration, data.withInline(declaration.isInline))

                    if (declaration.visibility == DescriptorVisibilities.LOCAL) {
                        val enclosingScope = data.currentClass
                            ?: enclosingClass?.scopeWithCounter
                            // File is required for K/N because file declarations are not split by classes.
                            ?: enclosingFile.scopeWithCounter
                        val index =
                            if (declaration.name.isSpecial || declaration.name in enclosingScope.usedLocalFunctionNames)
                                enclosingScope.counter++
                            else -1
                        localFunctions[declaration] =
                            LocalFunctionContext(declaration, index, enclosingScope.irElement as IrDeclarationContainer)
                        enclosingScope.usedLocalFunctionNames.add(declaration.name)
                    }
                }

                override fun visitConstructor(declaration: IrConstructor, data: Data) {
                    super.visitConstructor(declaration, data)

                    if (!declaration.constructedClass.isLocalNotInner()) return

                    localClassConstructors[declaration] = LocalClassConstructorContext(declaration, data.inInlineFunctionScope)
                }

                override fun visitClass(declaration: IrClass, data: Data) {
                    if (classesToLower?.contains(declaration) == false) return
                    super.visitClass(declaration, data.withCurrentClass(declaration))

                    if (!declaration.isLocalNotInner()) return

                    localClasses[declaration] = LocalClassContext(declaration, data.inInlineFunctionScope)
                }

                private val Data.inInlineFunctionScope: Boolean
                    get() = isInInlineFunction ||
                            generateSequence(container) { it.parent as? IrDeclaration }.any { it is IrFunction && it.isInline }
            }, Data(null, false))
        }
    }
}

// Local inner classes capture anything through outer
internal fun IrClass.isLocalNotInner(): Boolean = visibility == DescriptorVisibilities.LOCAL && !isInner
