/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import java.util.*

val jvmLocalDeclarationsPhase = makeIrFilePhase<CommonBackendContext>(
    { context ->
        LocalDeclarationsLowering(context, object : LocalNameProvider {
            override fun localName(declaration: IrDeclarationWithName): String =
                NameUtils.sanitizeAsJavaIdentifier(super.localName(declaration))
        }, Visibilities.PUBLIC)
    },
    name = "JvmLocalDeclarations",
    description = "Move local declarations to classes",
    prerequisite = setOf(sharedVariablesPhase)
)

interface LocalNameProvider {
    fun localName(declaration: IrDeclarationWithName): String =
        declaration.name.asString()

    companion object {
        val DEFAULT = object : LocalNameProvider {}
    }
}

val IrDeclaration.parentsWithSelf: Sequence<IrDeclarationParent>
    get() = generateSequence(this as? IrDeclarationParent) { (it as? IrDeclaration)?.parent }

val IrDeclaration.parents: Sequence<IrDeclarationParent>
    get() = parentsWithSelf.drop(1)

object BOUND_VALUE_PARAMETER: IrDeclarationOriginImpl("BOUND_VALUE_PARAMETER")

class LocalDeclarationsLowering(
    val context: BackendContext,
    val localNameProvider: LocalNameProvider = LocalNameProvider.DEFAULT,
    val loweredConstructorVisibility: Visibility = Visibilities.PRIVATE
) :
    DeclarationContainerLoweringPass {

    private object DECLARATION_ORIGIN_FIELD_FOR_CAPTURED_VALUE :
        IrDeclarationOriginImpl("FIELD_FOR_CAPTURED_VALUE", isSynthetic = true)

    private object STATEMENT_ORIGIN_INITIALIZER_OF_FIELD_FOR_CAPTURED_VALUE :
        IrStatementOriginImpl("INITIALIZER_OF_FIELD_FOR_CAPTURED_VALUE")

    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        if (irDeclarationContainer is IrDeclaration) {
            val parents = irDeclarationContainer.parents

            if (parents.any { it is IrFunction || it is IrField }) {
                // Lowering of non-local declarations handles all local declarations inside.
                // This declaration is local and shouldn't be considered.
                return
            }
        }

        // Continuous numbering across all declarations in the container.
        lambdasCount = 0

        irDeclarationContainer.transformDeclarationsFlat { memberDeclaration ->
            // TODO: may be do the opposite - specify the list of IR elements which need not to be transformed
            when (memberDeclaration) {
                is IrFunction -> LocalDeclarationsTransformer(memberDeclaration).lowerLocalDeclarations()
                is IrProperty -> LocalDeclarationsTransformer(memberDeclaration).lowerLocalDeclarations()
                is IrField -> LocalDeclarationsTransformer(memberDeclaration).lowerLocalDeclarations()
                is IrAnonymousInitializer -> LocalDeclarationsTransformer(memberDeclaration).lowerLocalDeclarations()
                is IrEnumEntry -> {
                    // The responsibility of pulling up classes for enum entries is on EnumClassLowering.
                    // Moreover, EnumClassLowering needs information whether a enum entry has its own class or not.
                    val correspondingClass = memberDeclaration.correspondingClass
                    memberDeclaration.correspondingClass = null
                    LocalDeclarationsTransformer(memberDeclaration).lowerLocalDeclarations().also {
                        memberDeclaration.correspondingClass = correspondingClass
                    }
                }
                // TODO: visit children as well
                else -> null
            }
        }
    }

    private var lambdasCount = 0

    private abstract class LocalContext {
        /**
         * @return the expression to get the value for given declaration, or `null` if [IrGetValue] should be used.
         */
        abstract fun irGet(startOffset: Int, endOffset: Int, valueDeclaration: IrValueDeclaration): IrExpression?
    }

    private abstract class LocalContextWithClosureAsParameters : LocalContext() {

        abstract val declaration: IrFunction
        abstract val transformedDeclaration: IrFunction

        val capturedValueToParameter: MutableMap<IrValueDeclaration, IrValueParameter> = HashMap()

        override fun irGet(startOffset: Int, endOffset: Int, valueDeclaration: IrValueDeclaration): IrExpression? {
            val parameter = capturedValueToParameter[valueDeclaration] ?: return null

            return IrGetValueImpl(startOffset, endOffset, parameter.type, parameter.symbol)
        }
    }

    private class LocalFunctionContext(override val declaration: IrFunction) : LocalContextWithClosureAsParameters() {
        lateinit var closure: Closure

        override lateinit var transformedDeclaration: IrSimpleFunction

        var index: Int = -1
    }

    private class LocalClassConstructorContext(override val declaration: IrConstructor) : LocalContextWithClosureAsParameters() {
        override lateinit var transformedDeclaration: IrConstructor
    }

    private class LocalClassContext(val declaration: IrClass) : LocalContext() {
        lateinit var closure: Closure

        val capturedValueToField: MutableMap<IrValueDeclaration, IrField> = HashMap()

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

    private inner class LocalDeclarationsTransformer(val memberDeclaration: IrDeclaration) {
        val localFunctions: MutableMap<IrFunction, LocalFunctionContext> = LinkedHashMap()
        val localClasses: MutableMap<IrClass, LocalClassContext> = LinkedHashMap()
        val localClassConstructors: MutableMap<IrConstructor, LocalClassConstructorContext> = LinkedHashMap()

        val transformedDeclarations = mutableMapOf<IrSymbolOwner, IrDeclaration>()

        val IrFunction.transformed: IrFunction?
            get() = transformedDeclarations[this] as IrFunction?

        val newParameterToOld: MutableMap<IrValueParameter, IrValueParameter> = mutableMapOf()
        val oldParameterToNew: MutableMap<IrValueParameter, IrValueParameter> = mutableMapOf()
        val newParameterToCaptured: MutableMap<IrValueParameter, IrValueSymbol> = mutableMapOf()

        fun lowerLocalDeclarations(): List<IrDeclaration> {
            collectLocalDeclarations()
            if (localFunctions.isEmpty() && localClasses.isEmpty()) return listOf(memberDeclaration)

            collectClosures()

            transformDeclarations()

            rewriteDeclarations()

            val rewrittenDeclarations = collectRewrittenDeclarations()
            rewrittenDeclarations.forEach {
                it.patchDeclarationParents(memberDeclaration.parent)
            }
            return rewrittenDeclarations
        }

        private fun collectRewrittenDeclarations(): ArrayList<IrDeclaration> =
            ArrayList<IrDeclaration>(localFunctions.size + localClasses.size + 1).apply {
                localFunctions.values.mapTo(this) {
                    val original = it.declaration
                    it.transformedDeclaration.apply {
                        this.body = original.body

                        original.valueParameters.filter { v -> v.defaultValue != null }.forEach { argument ->
                            val body = argument.defaultValue
                            oldParameterToNew[argument]!!.defaultValue = body
                        }
                    }
                }

                localClasses.values.mapTo(this) {
                    it.declaration.parent = memberDeclaration.parent
                    it.declaration
                }

                add(memberDeclaration)
            }

        private inner class FunctionBodiesRewriter(val localContext: LocalContext?) : IrElementTransformerVoid() {
            override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty) =
                // Both accessors extracted as closures.
                declaration.delegate.transform(this, null)

            override fun visitClass(declaration: IrClass) = if (declaration in localClasses) {
                // Replace local class definition with an empty composite.
                IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.irBuiltIns.unitType)
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
                    newCallee.descriptor,
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

                return IrFunctionReferenceImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type, // TODO functional type for transformed descriptor
                    newCallee.symbol,
                    newCallee.descriptor,
                    expression.typeArgumentsCount,
                    expression.origin
                ).also {
                    it.fillArguments2(expression, newCallee)
                    it.copyTypeArgumentsFrom(expression)
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

        private fun rewriteFunctionBody(irDeclaration: IrDeclaration, localContext: LocalContext?) {
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

            localClassContext.capturedValueToField.forEach { capturedValue, field ->
                val startOffset = irClass.startOffset
                val endOffset = irClass.endOffset
                irClass.declarations.add(field)

                for (constructorContext in constructorsCallingSuper) {
                    val blockBody = constructorContext.declaration.body as? IrBlockBody
                        ?: throw AssertionError("Unexpected constructor body: ${constructorContext.declaration.body}")
                    val capturedValueExpression = constructorContext.irGet(startOffset, endOffset, capturedValue)!!
                    blockBody.statements.add(
                        0,
                        IrSetFieldImpl(
                            startOffset, endOffset, field.symbol,
                            IrGetValueImpl(startOffset, endOffset, irClass.thisReceiver!!.symbol),
                            capturedValueExpression,
                            context.irBuiltIns.unitType,
                            STATEMENT_ORIGIN_INITIALIZER_OF_FIELD_FOR_CAPTURED_VALUE
                        )
                    )
                }
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

            rewriteFunctionBody(memberDeclaration, null)
        }

        private fun createNewCall(oldCall: IrCall, newCallee: IrFunction) =
            if (oldCall is IrCallWithShallowCopy)
                oldCall.shallowCopy(oldCall.origin, newCallee.symbol, oldCall.superQualifierSymbol)
            else
                IrCallImpl(
                    oldCall.startOffset, oldCall.endOffset,
                    newCallee.returnType,
                    newCallee.symbol,
                    newCallee.descriptor,
                    oldCall.typeArgumentsCount,
                    oldCall.origin, oldCall.superQualifierSymbol
                ).also {
                    it.copyTypeArgumentsFrom(oldCall)
                }

        private fun createNewCall(oldCall: IrConstructorCall, newCallee: IrConstructor) =
            IrConstructorCallImpl.fromSymbolOwner(
                oldCall.startOffset, oldCall.endOffset,
                newCallee.returnType,
                newCallee.symbol,
                oldCall.origin
            ).also {
                it.copyTypeArgumentsFrom(oldCall)
            }

        private fun transformDeclarations() {
            localFunctions.values.forEach {
                createLiftedDeclaration(it)
            }

            localClasses.values.forEach {
                createFieldsForCapturedValues(it)
            }

            localClassConstructors.values.forEach {
                createTransformedConstructorDeclaration(it)
            }
        }

        private fun suggestLocalName(declaration: IrDeclarationWithName): String {
            localFunctions[declaration]?.let {
                if (it.index >= 0)
                    return "lambda-${it.index}"
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
            val oldDeclaration = localFunctionContext.declaration as IrSimpleFunction

            val memberOwner = memberDeclaration.parent
            val newDescriptor = WrappedSimpleFunctionDescriptor(oldDeclaration.descriptor)
            val newSymbol = IrSimpleFunctionSymbolImpl(newDescriptor)
            val newName = generateNameForLiftedDeclaration(oldDeclaration, memberOwner)

            if (oldDeclaration.dispatchReceiverParameter != null) {
                throw AssertionError("local functions must not have dispatch receiver")
            }

            val newDispatchReceiverParameter = null

            // TODO: consider using fields to access the closure of enclosing class.
            val capturedValues = localFunctionContext.closure.capturedValues

            val newDeclaration = IrFunctionImpl(
                oldDeclaration.startOffset,
                oldDeclaration.endOffset,
                oldDeclaration.origin,
                newSymbol,
                newName,
                Visibilities.PRIVATE,
                Modality.FINAL,
                oldDeclaration.returnType,
                oldDeclaration.isInline,
                oldDeclaration.isExternal,
                oldDeclaration.isTailrec,
                oldDeclaration.isSuspend
            )
            newDescriptor.bind(newDeclaration)

            localFunctionContext.transformedDeclaration = newDeclaration

            newDeclaration.parent = memberOwner
            newDeclaration.copyTypeParametersFrom(oldDeclaration)
            newDeclaration.dispatchReceiverParameter = newDispatchReceiverParameter
            newDeclaration.extensionReceiverParameter = oldDeclaration.extensionReceiverParameter?.run {
                copyTo(newDeclaration).also {
                    newParameterToOld.putAbsentOrSame(it, this)
                }
            }

            newDeclaration.valueParameters += createTransformedValueParameters(capturedValues, oldDeclaration, newDeclaration)
            newDeclaration.recordTransformedValueParameters(localFunctionContext)

            newDeclaration.annotations.addAll(oldDeclaration.annotations)

            transformedDeclarations[oldDeclaration] = newDeclaration
        }

        private fun createTransformedValueParameters(
            capturedValues: List<IrValueSymbol>,
            oldDeclaration: IrFunction,
            newDeclaration: IrFunction
        ) = ArrayList<IrValueParameter>(capturedValues.size + oldDeclaration.valueParameters.size).apply {
            capturedValues.mapIndexedTo(this) { i, capturedValue ->
                val parameterDescriptor = WrappedValueParameterDescriptor()
                val p = capturedValue.owner
                IrValueParameterImpl(
                    p.startOffset, p.endOffset, BOUND_VALUE_PARAMETER, IrValueParameterSymbolImpl(parameterDescriptor),
                    suggestNameForCapturedValue(p), i, p.type, null, false, false
                ).also {
                    parameterDescriptor.bind(it)
                    it.parent = newDeclaration
                    newParameterToCaptured[it] = capturedValue
                }
            }

            oldDeclaration.valueParameters.mapTo(this) { v ->
                v.copyTo(newDeclaration, index = v.index + capturedValues.size).also {
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

            val newDeclaration = IrConstructorImpl(
                oldDeclaration.startOffset, oldDeclaration.endOffset, oldDeclaration.origin,
                newSymbol, oldDeclaration.name, loweredConstructorVisibility, oldDeclaration.returnType, oldDeclaration.isInline,
                oldDeclaration.isExternal, oldDeclaration.isPrimary
            )

            newDescriptor.bind(newDeclaration)

            constructorContext.transformedDeclaration = newDeclaration

            newDeclaration.parent = localClassContext.declaration
            newDeclaration.copyTypeParametersFrom(oldDeclaration)

            // TODO: should dispatch receiver be copied?
            newDeclaration.dispatchReceiverParameter = oldDeclaration.dispatchReceiverParameter?.copyTo(newDeclaration)?.also {
                newParameterToOld.putAbsentOrSame(it, oldDeclaration.dispatchReceiverParameter!!)
            }
            newDeclaration.extensionReceiverParameter = oldDeclaration.extensionReceiverParameter?.run {
                throw AssertionError("constructors can't have extension receiver")
            }

            newDeclaration.valueParameters += createTransformedValueParameters(capturedValues, oldDeclaration, newDeclaration)
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
            fieldType: IrType
        ): IrField {
            val descriptor = WrappedFieldDescriptor()
            val symbol = IrFieldSymbolImpl(descriptor)
            return IrFieldImpl(
                startOffset,
                endOffset,
                DECLARATION_ORIGIN_FIELD_FOR_CAPTURED_VALUE,
                symbol,
                name,
                fieldType,
                visibility,
                true,
                false,
                false
            ).also {
                descriptor.bind(it)
                it.parent = parent
            }
        }

        private fun createFieldsForCapturedValues(localClassContext: LocalClassContext) {
            val classDeclaration = localClassContext.declaration

            localClassContext.closure.capturedValues.forEach { capturedValue ->

                val irField = createFieldForCapturedValue(
                    classDeclaration.startOffset,
                    classDeclaration.endOffset,
                    suggestNameForCapturedValue(capturedValue.owner),
                    Visibilities.PRIVATE,
                    classDeclaration,
                    capturedValue.owner.type
                )

                localClassContext.capturedValueToField[capturedValue.owner] = irField
            }
        }

        private fun <K, V> MutableMap<K, V>.putAbsentOrSame(key: K, value: V) {
            val current = this.getOrPut(key) { value }

            if (current != value) {
                error("$current != $value")
            }
        }

        private fun suggestNameForCapturedValue(declaration: IrValueDeclaration): Name =
            if (declaration.name.isSpecial) {
                val oldNameStr = declaration.name.asString()
                oldNameStr.substring(1, oldNameStr.length - 1).synthesizedName
            } else
                declaration.name


        private fun collectClosures() {
            val annotator = ClosureAnnotator(memberDeclaration)

            localFunctions.forEach { declaration, context ->
                context.closure = annotator.getFunctionClosure(declaration)
            }

            localClasses.forEach { declaration, context ->
                context.closure = annotator.getClassClosure(declaration)
            }
        }

        private fun collectLocalDeclarations() {
            memberDeclaration.acceptChildrenVoid(object : IrElementVisitorVoid {

                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunction(declaration: IrFunction) {
                    declaration.acceptChildrenVoid(this)

                    if (declaration.visibility == Visibilities.LOCAL) {
                        val localFunctionContext = LocalFunctionContext(declaration)

                        localFunctions[declaration] = localFunctionContext

                        if (declaration.name.isSpecial) {
                            localFunctionContext.index = lambdasCount++
                        }
                    }
                }

                override fun visitConstructor(declaration: IrConstructor) {
                    declaration.acceptChildrenVoid(this)

                    assert(declaration.visibility != Visibilities.LOCAL)

                    if ((declaration.parent as IrClass).isInner) return

                    localClassConstructors[declaration] = LocalClassConstructorContext(declaration)
                }

                override fun visitClass(declaration: IrClass) {
                    declaration.acceptChildrenVoid(this)

                    if (declaration.isInner) return

                    val localClassContext = LocalClassContext(declaration)
                    localClasses[declaration] = localClassContext
                }
            })
        }
    }

}
