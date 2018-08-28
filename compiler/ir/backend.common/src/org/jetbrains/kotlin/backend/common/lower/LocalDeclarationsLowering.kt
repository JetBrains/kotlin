/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import java.util.*

interface LocalNameProvider {
    fun localName(descriptor: DeclarationDescriptor): String =
        descriptor.name.asString()

    companion object {
        val DEFAULT = object : LocalNameProvider {}
    }
}

class LocalDeclarationsLowering(
    val context: BackendContext,
    val localNameProvider: LocalNameProvider = LocalNameProvider.DEFAULT,
    val loweredConstructorVisibility: Visibility = Visibilities.PRIVATE
) :
    DeclarationContainerLoweringPass {

    private object DECLARATION_ORIGIN_FIELD_FOR_CAPTURED_VALUE :
        IrDeclarationOriginImpl("FIELD_FOR_CAPTURED_VALUE") {}

    private object STATEMENT_ORIGIN_INITIALIZER_OF_FIELD_FOR_CAPTURED_VALUE :
        IrStatementOriginImpl("INITIALIZER_OF_FIELD_FOR_CAPTURED_VALUE") {}

    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        if (irDeclarationContainer is IrDeclaration &&
            irDeclarationContainer.descriptor.parents.any { it is CallableDescriptor }
        ) {

            // Lowering of non-local declarations handles all local declarations inside.
            // This declaration is local and shouldn't be considered.
            return
        }

        // Continuous numbering across all declarations in the container.
        lambdasCount = 0

        irDeclarationContainer.declarations.transformFlat { memberDeclaration ->
            // TODO: may be do the opposite - specify the list of IR elements which need not to be transformed
            when (memberDeclaration) {
                is IrFunction -> LocalDeclarationsTransformer(memberDeclaration).lowerLocalDeclarations()
                is IrProperty -> LocalDeclarationsTransformer(memberDeclaration).lowerLocalDeclarations()
                is IrField -> LocalDeclarationsTransformer(memberDeclaration).lowerLocalDeclarations()
                is IrAnonymousInitializer -> LocalDeclarationsTransformer(memberDeclaration).lowerLocalDeclarations()
            // TODO: visit children as well
                else -> null
            }
        }
    }

    private var lambdasCount = 0

    private abstract class LocalContext {
        /**
         * @return the expression to get the value for given descriptor, or `null` if [IrGetValue] should be used.
         */
        abstract fun irGet(startOffset: Int, endOffset: Int, descriptor: ValueDescriptor): IrExpression?
    }

    private abstract class LocalContextWithClosureAsParameters : LocalContext() {

        abstract val declaration: IrFunction
        open val descriptor: FunctionDescriptor
            get() = declaration.descriptor

        abstract val transformedDescriptor: FunctionDescriptor
        abstract val transformedDeclaration: IrFunction

        val capturedValueToParameter: MutableMap<ValueDescriptor, IrValueParameter> = HashMap()

        override fun irGet(startOffset: Int, endOffset: Int, descriptor: ValueDescriptor): IrExpression? {
            val parameter = capturedValueToParameter[descriptor] ?: return null

            return IrGetValueImpl(startOffset, endOffset, parameter.type, parameter.symbol)
        }
    }

    private class LocalFunctionContext(override val declaration: IrFunction) : LocalContextWithClosureAsParameters() {
        lateinit var closure: Closure

        override lateinit var transformedDescriptor: FunctionDescriptor
        override lateinit var transformedDeclaration: IrSimpleFunction

        var index: Int = -1

        override fun toString(): String =
            "LocalFunctionContext for $descriptor"
    }

    private class LocalClassConstructorContext(override val declaration: IrConstructor) : LocalContextWithClosureAsParameters() {
        override val descriptor: ClassConstructorDescriptor
            get() = declaration.descriptor

        override lateinit var transformedDescriptor: ClassConstructorDescriptor
        override lateinit var transformedDeclaration: IrConstructor

        override fun toString(): String =
            "LocalClassConstructorContext for $descriptor"
    }

    private class LocalClassContext(val declaration: IrClass) : LocalContext() {
        val descriptor: ClassDescriptor
            get() = declaration.descriptor

        lateinit var closure: Closure

        val capturedValueToField: MutableMap<ValueDescriptor, IrField> = HashMap()

        override fun irGet(startOffset: Int, endOffset: Int, descriptor: ValueDescriptor): IrExpression? {
            val field = capturedValueToField[descriptor] ?: return null

            val receiver = declaration.thisReceiver!!
            return IrGetFieldImpl(
                startOffset, endOffset, field.symbol, field.type,
                receiver = IrGetValueImpl(startOffset, endOffset, receiver.type, receiver.symbol)
            )
        }

        override fun toString(): String =
            "LocalClassContext for $descriptor"
    }

    private class LocalClassMemberContext(val member: IrFunction, val classContext: LocalClassContext) : LocalContext() {
        override fun irGet(startOffset: Int, endOffset: Int, descriptor: ValueDescriptor): IrExpression? {
            val field = classContext.capturedValueToField[descriptor] ?: return null

            val receiver = member.dispatchReceiverParameter!!
            return IrGetFieldImpl(
                startOffset, endOffset, field.symbol, field.type,
                receiver = IrGetValueImpl(startOffset, endOffset, receiver.type, receiver.symbol)
            )
        }

    }

    private inner class LocalDeclarationsTransformer(val memberDeclaration: IrDeclaration) {
        val localFunctions: MutableMap<FunctionDescriptor, LocalFunctionContext> = LinkedHashMap()
        val localClasses: MutableMap<ClassDescriptor, LocalClassContext> = LinkedHashMap()
        val localClassConstructors: MutableMap<ClassConstructorDescriptor, LocalClassConstructorContext> = LinkedHashMap()

        val transformedDeclarations = mutableMapOf<DeclarationDescriptor, IrDeclaration>()

        val FunctionDescriptor.transformed: IrFunction?
            get() = transformedDeclarations[this] as IrFunction?

        val oldParameterToNew: MutableMap<ParameterDescriptor, IrValueParameter> = HashMap()
        val newParameterToOld: MutableMap<ParameterDescriptor, ParameterDescriptor> = HashMap()
        val newParameterToCaptured: MutableMap<ValueParameterDescriptor, IrValueSymbol> = HashMap()

        fun lowerLocalDeclarations(): List<IrDeclaration>? {
            collectLocalDeclarations()
            if (localFunctions.isEmpty() && localClasses.isEmpty()) return null

            collectClosures()

            transformDescriptors()

            rewriteDeclarations()

            val result = collectRewrittenDeclarations()
            return result
        }

        private fun collectRewrittenDeclarations(): ArrayList<IrDeclaration> =
            ArrayList<IrDeclaration>(localFunctions.size + localClasses.size + 1).apply {
                localFunctions.values.mapTo(this) {
                    val original = it.declaration
                    it.transformedDeclaration.apply {
                        this.body = original.body

                        original.descriptor.valueParameters.filter { it.declaresDefaultValue() }.forEach { argument ->
                            val body = original.getDefault(argument)!!
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

            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.descriptor in localClasses) {
                    // Replace local class definition with an empty composite.
                    return IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.irBuiltIns.unitType)
                } else {
                    return super.visitClass(declaration)
                }
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.descriptor in localFunctions) {
                    // Replace local function definition with an empty composite.
                    return IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.irBuiltIns.unitType)
                } else {
                    if (localContext is LocalClassContext && declaration.parent == localContext.declaration) {
                        return declaration.apply {
                            val classMemberLocalContext = LocalClassMemberContext(declaration, localContext)
                            transformChildrenVoid(FunctionBodiesRewriter(classMemberLocalContext))
                        }
                    } else {
                        return super.visitFunction(declaration)
                    }
                }
            }

            override fun visitConstructor(declaration: IrConstructor): IrStatement {
                // Body is transformed separately. See loop over constructors in rewriteDeclarations().

                val constructorContext = localClassConstructors[declaration.descriptor]
                if (constructorContext != null) {
                    return constructorContext.transformedDeclaration.apply {
                        this.body = declaration.body!!

                        declaration.descriptor.valueParameters.filter { it.declaresDefaultValue() }.forEach { argument ->
                            val body = declaration.getDefault(argument)!!
                            oldParameterToNew[argument]!!.defaultValue = body
                        }
                    }
                } else {
                    return super.visitConstructor(declaration)
                }
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val descriptor = expression.descriptor

                localContext?.irGet(expression.startOffset, expression.endOffset, descriptor)?.let {
                    return it
                }

                oldParameterToNew[descriptor]?.let {
                    return IrGetValueImpl(expression.startOffset, expression.endOffset, it.type, it.symbol)
                }

                return expression
            }

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                val oldCallee = expression.descriptor.original
                val newCallee = oldCallee.transformed ?: return expression

                val newCall = createNewCall(expression, newCallee).fillArguments(expression)

                return newCall
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                val oldCallee = expression.descriptor.original
                val newCallee = transformedDeclarations[oldCallee] as IrConstructor? ?: return expression

                return IrDelegatingConstructorCallImpl(
                    expression.startOffset, expression.endOffset,
                    context.irBuiltIns.unitType,
                    newCallee.symbol,
                    newCallee.descriptor,
                    expression.typeArgumentsCount
                ).also {
                    it.fillArguments(expression)
                    it.copyTypeArgumentsFrom(expression)
                }
            }

            private fun <T : IrMemberAccessExpression> T.fillArguments(oldExpression: IrMemberAccessExpression): T {

                mapValueParameters { newValueParameterDescriptor ->
                    val oldParameter = newParameterToOld[newValueParameterDescriptor]

                    if (oldParameter != null) {
                        oldExpression.getValueArgument(oldParameter as ValueParameterDescriptor)
                    } else {
                        // The callee expects captured value as argument.
                        val capturedValueSymbol =
                            newParameterToCaptured[newValueParameterDescriptor]
                                    ?: throw AssertionError("Non-mapped parameter $newValueParameterDescriptor")

                        val capturedValue = capturedValueSymbol.owner

                        val capturedValueDescriptor = capturedValueSymbol.descriptor
                        localContext?.irGet(
                            oldExpression.startOffset, oldExpression.endOffset,
                            capturedValueDescriptor
                        ) ?: run {
                            // Captured value is directly available for the caller.
                            val value = oldParameterToNew[capturedValueDescriptor] ?: capturedValue
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

                val oldCallee = expression.descriptor.original
                val newCallee = oldCallee.transformed ?: return expression

                val newCallableReference = IrFunctionReferenceImpl(
                    expression.startOffset, expression.endOffset,
                    expression.type, // TODO functional type for transformed descriptor
                    newCallee.symbol,
                    newCallee.descriptor,
                    expression.typeArgumentsCount,
                    expression.origin
                ).also {
                    it.fillArguments(expression)
                    it.copyTypeArgumentsFrom(expression)
                }

                return newCallableReference
            }

            override fun visitReturn(expression: IrReturn): IrExpression {
                expression.transformChildrenVoid(this)

                val oldReturnTarget = expression.returnTarget
                val newReturnTarget = oldReturnTarget.transformed ?: return expression

                return IrReturnImpl(
                    expression.startOffset, expression.endOffset,
                    context.irBuiltIns.nothingType,
                    newReturnTarget.symbol, expression.value
                )
            }

            override fun visitDeclarationReference(expression: IrDeclarationReference): IrExpression {
                if (expression.descriptor in transformedDeclarations) {
                    TODO()
                }
                return super.visitDeclarationReference(expression)
            }

            override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                if (declaration.descriptor in transformedDeclarations) {
                    TODO()
                }
                return super.visitDeclaration(declaration)
            }
        }

        private fun rewriteFunctionBody(irDeclaration: IrDeclaration, localContext: LocalContext?) {
            irDeclaration.transformChildrenVoid(FunctionBodiesRewriter(localContext))
        }

        private fun rewriteClassMembers(irClass: IrClass, localClassContext: LocalClassContext) {
            irClass.transformChildrenVoid(FunctionBodiesRewriter(localClassContext))

            val classDescriptor = irClass.descriptor
            val constructorsCallingSuper = classDescriptor.constructors
                .map { localClassConstructors[it]!! }
                .filter { it.declaration.callsSuper() }
            assert(constructorsCallingSuper.any(), { "Expected at least one constructor calling super; class: $classDescriptor" })

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

        private fun transformDescriptors() {
            localFunctions.values.forEach {
                createLiftedDescriptor(it)
            }

            localClasses.values.forEach {
                createFieldsForCapturedValues(it)
            }

            localClassConstructors.values.forEach {
                createTransformedConstructorDescriptor(it)
            }
        }

        private fun suggestLocalName(descriptor: DeclarationDescriptor): String {
            localFunctions[descriptor]?.let {
                if (it.index >= 0)
                    return "lambda-${it.index}"
            }

            return localNameProvider.localName(descriptor)
        }

        private fun generateNameForLiftedDeclaration(
            descriptor: DeclarationDescriptor,
            newOwner: DeclarationDescriptor
        ): Name =
            Name.identifier(
                descriptor.parentsWithSelf
                    .takeWhile { it != newOwner }
                    .toList().reversed()
                    .map { suggestLocalName(it) }
                    .joinToString(separator = "$")
            )

        private fun createLiftedDescriptor(localFunctionContext: LocalFunctionContext) {
            val oldDescriptor = localFunctionContext.descriptor

            val memberOwner = memberDeclaration.descriptor.containingDeclaration!!
            val newDescriptor = SimpleFunctionDescriptorImpl.create(
                memberOwner,
                oldDescriptor.annotations,
                generateNameForLiftedDeclaration(oldDescriptor, memberOwner),
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                oldDescriptor.source
            ).apply {
                isTailrec = oldDescriptor.isTailrec
                isSuspend = oldDescriptor.isSuspend
                // TODO: copy other properties or consider using `FunctionDescriptor.CopyBuilder`.
            }

            localFunctionContext.transformedDescriptor = newDescriptor

            if (oldDescriptor.dispatchReceiverParameter != null) {
                throw AssertionError("local functions must not have dispatch receiver")
            }

            val newDispatchReceiverParameter = null

            // Do not substitute type parameters for now.
            val newTypeParameters = oldDescriptor.typeParameters

            // TODO: consider using fields to access the closure of enclosing class.
            val capturedValues = localFunctionContext.closure.capturedValues

            val newValueParameters = createTransformedValueParameters(localFunctionContext, capturedValues)

            newDescriptor.initialize(
                oldDescriptor.extensionReceiverParameter?.copy(newDescriptor),
                newDispatchReceiverParameter,
                newTypeParameters,
                newValueParameters.map { it.descriptor as ValueParameterDescriptor },
                oldDescriptor.returnType,
                Modality.FINAL,
                Visibilities.PRIVATE
            )

            oldDescriptor.extensionReceiverParameter?.let {
                newParameterToOld.putAbsentOrSame(newDescriptor.extensionReceiverParameter!!, it)
            }

            localFunctionContext.transformedDeclaration = with(localFunctionContext.declaration) {
                IrFunctionImpl(startOffset, endOffset, origin, newDescriptor)
            }.apply {
                parent = memberDeclaration.parent
                returnType = localFunctionContext.declaration.returnType

                localFunctionContext.declaration.typeParameters.mapTo(this.typeParameters) {
                    IrTypeParameterImpl(it.startOffset, it.endOffset, it.origin, it.descriptor)
                        .apply { superTypes += it.superTypes }
                }

                localFunctionContext.declaration.extensionReceiverParameter?.let {
                    this.extensionReceiverParameter = IrValueParameterImpl(
                        it.startOffset,
                        it.endOffset,
                        it.origin,
                        descriptor.extensionReceiverParameter!!,
                        it.type,
                        null
                    )
                }
                this.valueParameters += newValueParameters
                recordTransformedValueParameters(localFunctionContext)
                transformedDeclarations[oldDescriptor] = this
            }
        }

        private fun createTransformedValueParameters(
            localContext: LocalContextWithClosureAsParameters,
            capturedValues: List<IrValueSymbol>
        )
                : List<IrValueParameter> {

            val oldDescriptor = localContext.descriptor
            val newDescriptor = localContext.transformedDescriptor

            val closureParametersCount = capturedValues.size
            val newValueParametersCount = closureParametersCount + oldDescriptor.valueParameters.size

            val newValueParameters = ArrayList<IrValueParameter>(newValueParametersCount).apply {
                capturedValues.mapIndexedTo(this) { i, capturedValue ->
                    val parameterDescriptor = createUnsubstitutedCapturedValueParameter(newDescriptor, capturedValue.descriptor, i).apply {
                        newParameterToCaptured[this] = capturedValue
                    }
                    capturedValue.owner.copy(parameterDescriptor)
                }

                localContext.declaration.valueParameters.mapIndexedTo(this) { i, oldValueParameter ->
                    val parameterDescriptor = createUnsubstitutedParameter(
                        newDescriptor,
                        oldValueParameter.descriptor as ValueParameterDescriptor,
                        closureParametersCount + i
                    ).apply {
                        newParameterToOld.putAbsentOrSame(this, oldValueParameter.descriptor)
                    }
                    oldValueParameter.copy(parameterDescriptor)
                }
            }
            return newValueParameters
        }

        private fun IrFunction.recordTransformedValueParameters(localContext: LocalContextWithClosureAsParameters) {

            valueParameters.forEach {
                val capturedValue = newParameterToCaptured[it.descriptor]
                if (capturedValue != null) {
                    localContext.capturedValueToParameter[capturedValue.descriptor] = it
                }
            }

            (listOfNotNull(dispatchReceiverParameter, extensionReceiverParameter) + valueParameters).forEach {
                val oldParameter = newParameterToOld[it.descriptor]
                if (oldParameter != null) {
                    oldParameterToNew.putAbsentOrSame(oldParameter, it)
                }
            }

        }

        private fun createTransformedConstructorDescriptor(constructorContext: LocalClassConstructorContext) {
            val oldDescriptor = constructorContext.descriptor
            val localClassContext = localClasses[oldDescriptor.containingDeclaration]!!
            val newDescriptor = ClassConstructorDescriptorImpl.create(
                localClassContext.descriptor,
                Annotations.EMPTY, oldDescriptor.isPrimary, oldDescriptor.source
            )

            constructorContext.transformedDescriptor = newDescriptor

            // Do not substitute type parameters for now.
            val newTypeParameters = oldDescriptor.typeParameters

            val capturedValues = localClasses[oldDescriptor.containingDeclaration]!!.closure.capturedValues

            val newValueParameters = createTransformedValueParameters(constructorContext, capturedValues)

            newDescriptor.initialize(
                newValueParameters.map { it.descriptor as ValueParameterDescriptor },
                loweredConstructorVisibility,
                newTypeParameters
            )
            newDescriptor.returnType = oldDescriptor.returnType

            oldDescriptor.dispatchReceiverParameter?.let {
                newParameterToOld.putAbsentOrSame(newDescriptor.dispatchReceiverParameter!!, it)
            }

            oldDescriptor.extensionReceiverParameter?.let {
                throw AssertionError("constructors can't have extension receiver")
            }

            constructorContext.transformedDeclaration = with(constructorContext.declaration) {
                IrConstructorImpl(startOffset, endOffset, origin, newDescriptor).also { it.returnType = returnType }
            }.apply {
                parent = constructorContext.declaration.parent
                constructorContext.declaration.dispatchReceiverParameter?.let {
                    this.dispatchReceiverParameter = IrValueParameterImpl(
                        it.startOffset,
                        it.endOffset,
                        it.origin,
                        descriptor.dispatchReceiverParameter!!,
                        it.type,
                        null
                    )
                }

                this.valueParameters += newValueParameters
                recordTransformedValueParameters(constructorContext)
                transformedDeclarations[oldDescriptor] = this
            }
        }

        private fun createFieldsForCapturedValues(localClassContext: LocalClassContext) {
            val classDescriptor = localClassContext.descriptor

            localClassContext.closure.capturedValues.forEach { capturedValue ->
                val fieldDescriptor = PropertyDescriptorImpl.create(
                    classDescriptor,
                    Annotations.EMPTY,
                    Modality.FINAL,
                    Visibilities.PRIVATE,
                    /* isVar = */ false,
                    suggestNameForCapturedValue(capturedValue.descriptor),
                    CallableMemberDescriptor.Kind.SYNTHESIZED,
                    SourceElement.NO_SOURCE,
                    /* lateInit = */ false,
                    /* isConst = */ false,
                    /* isExpect = */ false,
                    /* isActual = */ false,
                    /* isExternal = */ false,
                    /* isDelegated = */ false
                )

                fieldDescriptor.initialize(/* getter = */ null, /* setter = */ null)

                val extensionReceiverParameter: ReceiverParameterDescriptor? = null

                fieldDescriptor.setType(
                    capturedValue.descriptor.type,
                    emptyList<TypeParameterDescriptor>(),
                    classDescriptor.thisAsReceiverParameter,
                    extensionReceiverParameter
                )

                localClassContext.capturedValueToField[capturedValue.descriptor] = IrFieldImpl(
                    localClassContext.declaration.startOffset, localClassContext.declaration.endOffset,
                    DECLARATION_ORIGIN_FIELD_FOR_CAPTURED_VALUE,
                    fieldDescriptor, capturedValue.owner.type
                ).apply {
                    parent = localClassContext.declaration
                }
            }
        }

        private fun <K, V> MutableMap<K, V>.putAbsentOrSame(key: K, value: V) {
            val current = this.getOrPut(key, { value })

            if (current != value) {
                error("$current != $value")
            }
        }

        private fun suggestNameForCapturedValue(valueDescriptor: ValueDescriptor): Name =
            if (valueDescriptor.name.isSpecial) {
                val oldNameStr = valueDescriptor.name.asString()
                oldNameStr.substring(1, oldNameStr.length - 1).synthesizedName
            } else
                valueDescriptor.name

        private fun createUnsubstitutedCapturedValueParameter(
            newParameterOwner: CallableMemberDescriptor,
            valueDescriptor: ValueDescriptor,
            index: Int
        ): ValueParameterDescriptor =
            ValueParameterDescriptorImpl(
                newParameterOwner, null, index,
                valueDescriptor.annotations,
                suggestNameForCapturedValue(valueDescriptor),
                valueDescriptor.type,
                false, false, false, null, valueDescriptor.source
            )

        private fun createUnsubstitutedParameter(
            newParameterOwner: CallableMemberDescriptor,
            valueParameterDescriptor: ValueParameterDescriptor,
            newIndex: Int
        ): ValueParameterDescriptor =
            valueParameterDescriptor.copy(newParameterOwner, valueParameterDescriptor.name, newIndex)


        private fun collectClosures() {
            val annotator = ClosureAnnotator(memberDeclaration)
            localFunctions.forEach { descriptor, context ->
                context.closure = annotator.getFunctionClosure(descriptor)
            }

            localClasses.forEach { descriptor, context ->
                context.closure = annotator.getClassClosure(descriptor)
            }
        }

        private fun collectLocalDeclarations() {
            memberDeclaration.acceptChildrenVoid(object : IrElementVisitorVoid {

                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                private fun ClassDescriptor.declaredInFunction() = when (this.containingDeclaration) {
                    is CallableDescriptor -> true
                    is ClassDescriptor -> false
                    is PackageFragmentDescriptor -> false
                    else -> TODO(this.toString() + "\n" + this.containingDeclaration.toString())
                }

                override fun visitFunction(declaration: IrFunction) {
                    declaration.acceptChildrenVoid(this)

                    val descriptor = declaration.descriptor

                    if (descriptor.visibility == Visibilities.LOCAL) {
                        val localFunctionContext = LocalFunctionContext(declaration)

                        localFunctions[descriptor] = localFunctionContext

                        if (descriptor.name.isSpecial) {
                            localFunctionContext.index = lambdasCount++
                        }
                    }

                }

                override fun visitConstructor(declaration: IrConstructor) {
                    declaration.acceptChildrenVoid(this)

                    val descriptor = declaration.descriptor
                    assert(declaration.visibility != Visibilities.LOCAL)

                    if ((declaration.parent as IrClass).isInner) return

                    localClassConstructors[descriptor] = LocalClassConstructorContext(declaration)
                }

                override fun visitClass(declaration: IrClass) {
                    declaration.acceptChildrenVoid(this)

                    val descriptor = declaration.descriptor

                    if (declaration.isInner) return

                    // Local nested classes can only be inner.
                    assert(descriptor.declaredInFunction())

                    val localClassContext = LocalClassContext(declaration)
                    localClasses[descriptor] = localClassContext
                }
            })
        }
    }

}

private fun IrValueDeclaration.copy(newDescriptor: ParameterDescriptor): IrValueParameter = IrValueParameterImpl(
    startOffset,
    endOffset,
    IrDeclarationOrigin.DEFINED,
    newDescriptor,
    type,
    (this as? IrValueParameter)?.varargElementType
)
