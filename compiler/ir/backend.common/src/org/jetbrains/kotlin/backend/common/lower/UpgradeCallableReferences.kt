/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.runIf

open class UpgradeCallableReferences(
    val context: LoweringContext,
    val upgradeFunctionReferencesAndLambdas: Boolean = true,
    val upgradePropertyReferences: Boolean = true,
    val upgradeLocalDelegatedPropertyReferences: Boolean = true,
    val upgradeSamConversions: Boolean = true,
    val upgradeExtractedAdaptedBlocks: Boolean = false,
    val castDispatchReceiver: Boolean = true,
    val generateFakeAccessorsForReflectionProperty: Boolean = false,
) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.transform(UpgradeTransformer(), irFile)
    }

    fun lower(irFunction: IrFunction) {
        irFunction.transform(UpgradeTransformer(), irFunction)
    }

    open fun IrTransformer<IrDeclarationParent>.processCallExpression(expression: IrCall, data: IrDeclarationParent) =
        this.visitFunctionAccess(expression, data)


    private data class AdaptedBlock(
        val function: IrSimpleFunction,
        val reference: IrFunctionReference,
        val samConversionType: IrType?,
        val referenceType: IrType,
    )

    private data class CapturedValue(
        val name: Name,
        val type: IrType,
        val correspondingParameter: IrValueParameter?,
        val expression: IrExpression,
    )

    private inner class UpgradeTransformer : IrTransformer<IrDeclarationParent>() {
        private fun IrClass?.isRestrictedSuspension(): Boolean {
            if (this == null) return false
            return hasAnnotation(StandardClassIds.Annotations.RestrictsSuspension) ||
                    getAllSuperclasses().any { hasAnnotation(StandardClassIds.Annotations.RestrictsSuspension) }
        }

        private fun IrFunction.isRestrictedSuspensionFunction(): Boolean {
            return parameters.any {
                return it.kind == IrParameterKind.ExtensionReceiver && it.type.classOrNull?.owner.isRestrictedSuspension()
            }
        }

        private fun IrFunction.flattenParameters() {
            for (parameter in parameters) {
                require(parameter.kind != IrParameterKind.DispatchReceiver) { "No dispatch receiver allowed in wrappers" }
                if (parameter.kind == IrParameterKind.ExtensionReceiver) parameter.origin = BOUND_RECEIVER_PARAMETER
                parameter.kind = IrParameterKind.Regular
            }
        }

        override fun visitCall(expression: IrCall, data: IrDeclarationParent): IrElement = processCallExpression(expression, data)

        override fun visitFunctionExpression(expression: IrFunctionExpression, data: IrDeclarationParent): IrElement {
            expression.transformChildren(this, data)
            if (!upgradeFunctionReferencesAndLambdas) return expression
            val isRestrictedSuspension = expression.function.isRestrictedSuspensionFunction()
            expression.function.flattenParameters()
            return IrRichFunctionReferenceImpl(
                startOffset = expression.startOffset,
                endOffset = expression.endOffset,
                type = expression.type,
                reflectionTargetSymbol = null,
                overriddenFunctionSymbol = expression.type.classOrFail.owner.selectSAMOverriddenFunction().symbol,
                invokeFunction = expression.function,
                origin = expression.origin,
                isRestrictedSuspension = isRestrictedSuspension,
            )
        }

        override fun visitElement(element: IrElement, data: IrDeclarationParent): IrElement {
            element.transformChildren(this, element as? IrDeclarationParent ?: data)
            return element
        }

        // IrTransformer defines this to not calling visitElement, which leads to incorrect parent creation
        override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclarationParent): IrStatement {
            return context.irFactory.stageController.restrictTo(declaration) {
                visitElement(declaration, data) as IrStatement
            }
        }

        override fun visitFile(declaration: IrFile, data: IrDeclarationParent): IrFile {
            return visitElement(declaration, data) as IrFile
        }

        private fun IrType.arrayDepth(): Int {
            if (this !is IrSimpleType) return 0
            return when (classOrNull) {
                context.irBuiltIns.arrayClass -> 1 + (arguments[0].typeOrNull?.arrayDepth() ?: 0)
                in context.irBuiltIns.arrays -> 1
                else -> 0
            }
        }

        private fun hasVarargConversion(wrapper: IrSimpleFunction, target: IrSimpleFunction): Boolean {
            return target.parameters.zip(wrapper.parameters)
                .takeWhile { (original, _) -> original.defaultValue == null }
                .any { (original, adapted) ->
                    // if original is (vararg x: T) than adapted can be either (Array<T> or T). conversion happened only in later case.
                    original.isVararg && original.type.arrayDepth() == adapted.type.arrayDepth() + 1
                }
        }

        private val blockReferenceOrigins = setOf(
            IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE, IrStatementOrigin.SUSPEND_CONVERSION,
            IrStatementOrigin.LAMBDA, IrStatementOrigin.INLINE_LAMBDA, IrStatementOrigin.FUN_INTERFACE_CONSTRUCTOR_REFERENCE, IrStatementOrigin.ANONYMOUS_FUNCTION,
        )

        // TODO delete once the lowering is moved
        private val usedLambdas = mutableSetOf<IrFunction>()

        // TODO delete once the lowering is moved
        override fun visitClass(declaration: IrClass, data: IrDeclarationParent): IrStatement {
            return super.visitClass(declaration, data).also { declaration.declarations.removeIf { it in usedLambdas } }
        }

        private fun IrBlock.parseAdaptedBlock() : AdaptedBlock? {
            if (origin !in blockReferenceOrigins) return null
            if (statements.size != 2) return null
            val (function, reference) = statements
            return when (reference) {
                is IrFunctionReference -> {
                    when (function) {
                        is IrSimpleFunction -> AdaptedBlock(function, reference, null, reference.type)
                        is IrContainerExpression if upgradeExtractedAdaptedBlocks && function.statements.isEmpty() -> {
                            val lambda = reference.symbol.owner as IrSimpleFunction
                            val lambdaToAdd = if (usedLambdas.add(lambda)) lambda else lambda.deepCopyWithSymbols()
                            AdaptedBlock(lambdaToAdd, reference, null, reference.type)
                        }
                        else -> null
                    }
                }
                is IrTypeOperatorCall -> {
                    if (function !is IrSimpleFunction) return null
                    if (reference.operator != IrTypeOperator.SAM_CONVERSION) return null
                    val argument = reference.argument as? IrFunctionReference ?: return null
                    if (upgradeSamConversions) {
                        AdaptedBlock(function, argument, null, reference.typeOperand)
                    } else {
                        AdaptedBlock(function, argument, reference.typeOperand, argument.type)
                    }
                }
                else -> null
            }
        }

        override fun visitBlock(expression: IrBlock, data: IrDeclarationParent): IrExpression {
            if (!upgradeFunctionReferencesAndLambdas) return super.visitBlock(expression, data)
            val (function, reference, samType, referenceType) = expression.parseAdaptedBlock() ?: return super.visitBlock(expression, data)
            function.transformChildren(this, function)
            function.setDeclarationsParent(data)
            function.visibility = DescriptorVisibilities.LOCAL
            reference.transformChildren(this, data)
            val isRestrictedSuspension = function.isRestrictedSuspensionFunction()
            function.flattenParameters()
            val (boundParameters, unboundParameters) = function.parameters.partition { reference.arguments[it.indexInParameters] != null }
            function.parameters = boundParameters + unboundParameters
            val reflectionTarget = reference.reflectionTarget.takeUnless { expression.origin.isLambda }
            return IrRichFunctionReferenceImpl(
                startOffset = expression.startOffset,
                endOffset = expression.endOffset,
                type = referenceType,
                reflectionTargetSymbol = reflectionTarget,
                overriddenFunctionSymbol = referenceType.classOrFail.owner.selectSAMOverriddenFunction().symbol,
                invokeFunction = function,
                origin = reference.origin,
                hasSuspendConversion = reflectionTarget != null && reflectionTarget.isSuspend == false && function.isSuspend,
                hasUnitConversion = reflectionTarget != null && !reflectionTarget.owner.returnType.isUnit() && function.returnType.isUnit(),
                hasVarargConversion = reflectionTarget is IrSimpleFunctionSymbol && hasVarargConversion(function, reflectionTarget.owner),
                isRestrictedSuspension = isRestrictedSuspension,
            ).apply {
                boundValues.addAll(reference.arguments.filterNotNull())
                copyNecessaryAttributes(reference, this)
            }.let {
                if (samType != null) {
                    IrTypeOperatorCallImpl(
                        startOffset = expression.startOffset, endOffset = expression.endOffset,
                        type = samType,
                        operator = IrTypeOperator.SAM_CONVERSION,
                        typeOperand = samType,
                        argument = it
                    )
                } else {
                    it
                }
            }
        }

        override fun visitTypeOperator(expression: IrTypeOperatorCall, data: IrDeclarationParent): IrExpression {
            if (upgradeSamConversions && expression.operator == IrTypeOperator.SAM_CONVERSION) {
                expression.transformChildren(this, data)
                val argument = expression.argument
                if (argument !is IrRichFunctionReference) return expression
                return argument.apply {
                    startOffset = expression.startOffset
                    endOffset = expression.endOffset
                    type = expression.typeOperand
                    overriddenFunctionSymbol = expression.typeOperand.classOrFail.owner.selectSAMOverriddenFunction().symbol
                }
            }
            return super.visitTypeOperator(expression, data)
        }

        private fun IrCallableReference<*>.getCapturedValues() = getArgumentsWithIr().map {
            CapturedValue(it.first.name, it.second.type, it.first, it.second)
        }

        override fun visitFunctionReference(expression: IrFunctionReference, data: IrDeclarationParent): IrExpression {
            expression.transformChildren(this, data)
            fixCallableReferenceComingFromKlib(expression)
            if (!upgradeFunctionReferencesAndLambdas) return expression
            val arguments = expression.getCapturedValues()
            return IrRichFunctionReferenceImpl(
                startOffset = expression.startOffset,
                endOffset = expression.endOffset,
                type = expression.type,
                reflectionTargetSymbol = (expression.reflectionTarget ?: expression.symbol).takeUnless { expression.origin.isLambda },
                overriddenFunctionSymbol = expression.type.classOrFail.owner.selectSAMOverriddenFunction().symbol,
                invokeFunction = expression.wrapFunction(arguments, data, expression.symbol.owner),
                origin = expression.origin,
                isRestrictedSuspension = expression.symbol.owner.isRestrictedSuspensionFunction(),
            ).apply {
                boundValues += arguments.map { it.expression }
            }
        }

        override fun visitPropertyReference(expression: IrPropertyReference, data: IrDeclarationParent): IrExpression {
            expression.transformChildren(this, data)
            fixCallableReferenceComingFromKlib(expression)
            if (!upgradePropertyReferences) return expression
            val getter = expression.getter?.owner
            val setter = expression.setter?.owner
            val getterFun: IrSimpleFunction
            val setterFun: IrSimpleFunction?
            val boundValues: List<IrExpression>

            if (getter != null) {
                if (generateFakeAccessorsForReflectionProperty && expression.origin == IrStatementOrigin.PROPERTY_REFERENCE_FOR_DELEGATE) {
                    boundValues = emptyList()
                    getterFun = getter.let {
                        expression.buildReflectionPropertyAccessorWithoutBody(
                            emptyList(), data, it.name, it.isSuspend, isPropertySetter = false
                        )
                    }
                    setterFun = setter?.let {
                        expression.buildReflectionPropertyAccessorWithoutBody(
                            emptyList(), data, it.name, it.isSuspend, isPropertySetter = true
                        )
                    }
                } else {
                    val getterArguments = if (getter.hasMissingObjectDispatchReceiver()) {
                        val objectClass = expression.symbol.owner.parentAsClass
                        val dispatchReceiver = IrGetObjectValueImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET, objectClass.defaultType, objectClass.symbol
                        )
                        val fakeParameter = CapturedValue(SpecialNames.THIS, objectClass.defaultType, null, dispatchReceiver)
                        listOf(fakeParameter) + expression.getCapturedValues()
                    } else {
                        expression.getCapturedValues()
                    }
                    boundValues = getterArguments.map { it.expression }
                    getterFun = expression.wrapFunction(getterArguments, data, getter, isPropertySetter = false)
                    setterFun = runIf(expression.type.isKMutableProperty() && setter != null) {
                        requireNotNull(setter)
                        val parameterIndexShift = when {
                            getter.hasMissingObjectDispatchReceiver() && !setter.hasMissingObjectDispatchReceiver() -> 1
                            setter.hasMissingObjectDispatchReceiver() && !getter.hasMissingObjectDispatchReceiver() -> -1
                            else -> 0
                        }
                        val setterArguments = getterArguments.map {
                            it.copy(
                                correspondingParameter = when (val p = it.correspondingParameter) {
                                    null -> setter.dispatchReceiverParameter // maybe null if both hasMissingObjectDispatchReceiver()
                                    else -> setter.parameters.getOrNull(p.indexInParameters + parameterIndexShift)
                                }
                            )
                        }
                        expression.wrapFunction(setterArguments, data, setter, isPropertySetter = true)
                    }
                }
            } else {
                boundValues = listOfNotNull(expression.dispatchReceiver)
                val arguments = boundValues.map {
                    CapturedValue(SpecialNames.THIS, it.type, null, it)
                }
                val field = expression.field!!.owner
                getterFun = expression.wrapField(arguments, data, field, isPropertySetter = false)
                setterFun = runIf(expression.type.isKMutableProperty()) {
                    expression.wrapField(arguments, data, field, isPropertySetter = true)
                }
            }
            return IrRichPropertyReferenceImpl(
                startOffset = expression.startOffset,
                endOffset = expression.endOffset,
                type = expression.type,
                reflectionTargetSymbol = expression.symbol,
                getterFunction = getterFun,
                setterFunction = setterFun,
                origin = expression.origin,
            ).apply {
                this.boundValues.addAll(boundValues)
                copyNecessaryAttributes(expression, this)
            }
        }

        /**
         * KLIBs don't contain enough information for initializing the correct shape for [IrFunctionReference]s and [IrPropertyReference]s.
         *
         * For example, consider the following code:
         *
         * ```kotlin
         * class C {
         *     fun foo() {}
         * }
         *
         * fun bar() {}
         * ```
         *
         * Function references `C::foo` and `::bar` will both be serialized (and deserialized) as having the following shape:
         * ```
         * dispatch_receiver = null
         * extension_receiver = null
         * value_argument = []
         * ```
         *
         * However, `C::foo` has unbound dispatch receiver, while `::bar` doesn't have any dispatch receiver.
         * To be able to adopt the new value parameter API ([KT-71850](https://youtrack.jetbrains.com/issue/KT-71850)),
         * we have to be able to distinguish these two situations, because for `C::foo` the target function's [IrFunction.parameters]
         * will be [[dispatch receiver]], while for `::bar` the target function's [IrFunction.parameters] will be an empty list,
         * and [IrFunctionReference.arguments] must always match the target function's [IrFunction.parameters] list.
         *
         * The same applies to [IrPropertyReference].
         *
         * Because existing KLIBs already don't contain enough information for setting the correct shape, the following hack is used:
         * After linking we visit callable references and update their shape from the linked target function/property.
         *
         * See [KT-71849](https://youtrack.jetbrains.com/issue/KT-71849).
         */
        private fun fixCallableReferenceComingFromKlib(reference: IrCallableReference<*>) {
            reference.initializeTargetShapeFromSymbol()
        }

        override fun visitLocalDelegatedPropertyReference(
            expression: IrLocalDelegatedPropertyReference,
            data: IrDeclarationParent
        ): IrExpression {
            expression.transformChildren(this, data)
            if (!upgradeLocalDelegatedPropertyReferences) return expression
            return IrRichPropertyReferenceImpl(
                startOffset = expression.startOffset,
                endOffset = expression.endOffset,
                type = expression.type,
                reflectionTargetSymbol = expression.symbol,
                getterFunction = expression.getter.owner.let {
                    expression.buildUnsupportedForLocalFunction(emptyList(), data, it.name, it.isSuspend, isPropertySetter = false)
                },
                setterFunction = expression.setter?.owner?.let {
                    expression.buildUnsupportedForLocalFunction(emptyList(), data, it.name, it.isSuspend, isPropertySetter = true)
                },
                origin = expression.origin
            ).apply {
                copyNecessaryAttributes(expression, this)
            }
        }

        private fun IrCallableReference<*>.buildUnsupportedForLocalFunction(
            captured: List<CapturedValue>,
            parent: IrDeclarationParent,
            name: Name,
            isSuspend: Boolean,
            isPropertySetter: Boolean,
        ) = buildWrapperFunction(captured, parent, name, isSuspend, isPropertySetter) { _, _ ->
            +irCall(this@UpgradeCallableReferences.context.symbols.throwUnsupportedOperationException).apply {
                arguments[0] = irString("Not supported for local property reference.")
            }
        }.apply {
            returnType = context.irBuiltIns.nothingType
        }

        private fun IrCallableReference<*>.buildReflectionPropertyAccessorWithoutBody(
            captured: List<CapturedValue>,
            parent: IrDeclarationParent,
            name: Name,
            isSuspend: Boolean,
            isPropertySetter: Boolean,
        ) = buildWrapperFunction(captured, parent, name, isSuspend, isPropertySetter, body = null)

        private fun IrCallableReference<*>.buildWrapperFunction(
            captured: List<CapturedValue>,
            parent: IrDeclarationParent,
            name: Name,
            isSuspend: Boolean,
            isPropertySetter: Boolean,
            body: (IrBlockBodyBuilder.(List<IrValueParameter>, IrType) -> Unit)?,
        ): IrSimpleFunction {
            val referenceType = this@buildWrapperFunction.type as IrSimpleType
            val referenceTypeArgs = referenceType.arguments.map { it.typeOrNull ?: context.irBuiltIns.anyNType }
            val unboundArgTypes = if (isPropertySetter) referenceTypeArgs else referenceTypeArgs.dropLast(1)
            // normally, it can't be empty. This is a workaround for plugin bugs, possibly already serialized in klibs
            val returnType = if (isPropertySetter) context.irBuiltIns.unitType else referenceTypeArgs.lastOrNull() ?: context.irBuiltIns.anyNType
            val func = context.irFactory.buildFun {
                setSourceRange(this@buildWrapperFunction)
                origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                this.name = name
                visibility = DescriptorVisibilities.LOCAL
                this.returnType = returnType
                this.isSuspend = isSuspend
            }.apply {
                this.parent = parent
                for (arg in captured) {
                    addValueParameter {
                        this.name = arg.name
                        this.type = arg.type
                    }
                }
                var index = 0
                for (type in unboundArgTypes) {
                    addValueParameter {
                        this.name = Name.identifier("p${index++}")
                        this.type = type
                    }
                }
                if (body != null) {
                    this.body = context.createIrBuilder(symbol).run {
                        irBlockBody {
                            body(parameters, returnType)
                        }
                    }
                }
            }
            return func
        }

        private fun IrPropertyReference.wrapField(
            captured: List<CapturedValue>,
            parent: IrDeclarationParent,
            field: IrField,
            isPropertySetter: Boolean
        ): IrSimpleFunction {
            return buildWrapperFunction(
                captured,
                parent,
                name = Name.special("<${if (isPropertySetter) "set-" else "get-"}${symbol.owner.name}>"),
                isSuspend = false,
                isPropertySetter = isPropertySetter
            ) { params , expectedReturnType->
                var index = 0
                val fieldReceiver = when {
                    field.isStatic -> null
                    else -> irGet(params[index++])
                }
                val exprToReturn = if (isPropertySetter) {
                    irSetField(fieldReceiver, field, irGet(params[index++]))
                } else {
                    irGetField(fieldReceiver, field)
                }.implicitCastIfNeededTo(expectedReturnType)
                require(index == params.size)
                +irReturn(exprToReturn)
            }
        }

        private fun IrCallableReference<*>.wrapFunction(
            captured: List<CapturedValue>,
            parent: IrDeclarationParent,
            referencedFunction: IrFunction,
            isPropertySetter: Boolean = false
        ): IrSimpleFunction {
            return buildWrapperFunction(
                captured,
                parent,
                referencedFunction.name,
                referencedFunction.isSuspend,
                isPropertySetter
            ) { wrapperFunctionParameters, expectedReturnType ->
                // Unfortunately, some plugins sometimes generate the wrong number of arguments in references
                // we already have such klib, so need to handle it. We just ignore extra type parameters
                val allTypeParameters = referencedFunction.allTypeParameters
                val cleanedTypeArguments = allTypeParameters.indices.map { typeArguments.getOrNull(it) ?: context.irBuiltIns.anyNType }
                val typeArgumentsMap = allTypeParameters.indices.associate {
                    allTypeParameters[it].symbol to cleanedTypeArguments[it]
                }
                val builder = this@UpgradeCallableReferences
                    .context
                    .createIrBuilder(symbol)
                    .at(this@wrapFunction)

                val forwardOrder = orderParametersToForward(
                    captured = captured,
                    referencedFunctionParameters = referencedFunction.parameters,
                    wrapperFunctionParameters = wrapperFunctionParameters
                )

                val typeSubstitutor = IrTypeSubstitutor(typeArgumentsMap, allowEmptySubstitution = true)
                    .chainedWith(run {
                        val dispatchReceiverParameterClass = referencedFunction.dispatchReceiverParameter?.type?.classOrNull ?: return@run null
                        val dispatchReceiverType = forwardOrder[0].type as? IrSimpleType
                        AbstractIrTypeSubstitutor.forSuperClass(
                            dispatchReceiverParameterClass,
                            if (dispatchReceiverType?.classifier?.isSubtypeOfClass(dispatchReceiverParameterClass) != true) {
                                dispatchReceiverParameterClass.starProjectedType
                            } else {
                                dispatchReceiverType
                            }
                        )
                    })

                val exprToReturn =
                    builder.irCall(
                        referencedFunction.symbol,
                        type = typeSubstitutor.substitute(referencedFunction.returnType),
                        typeArguments = cleanedTypeArguments,
                    ).apply {
                        for ((parameter, forwardParameter) in referencedFunction.parameters.zip(forwardOrder)) {
                            val rawArgument = builder.irGet(forwardParameter)
                            this.arguments[parameter] =
                                if (!castDispatchReceiver && parameter.kind == IrParameterKind.DispatchReceiver) rawArgument
                                else rawArgument.implicitCastIfNeededTo(typeSubstitutor.substitute(parameter.type))
                        }
                    }.implicitCastIfNeededTo(expectedReturnType)
                +irReturn(exprToReturn)
            }
        }

        private fun orderParametersToForward(
            captured: List<CapturedValue>,
            referencedFunctionParameters: List<IrValueParameter>,
            wrapperFunctionParameters: List<IrValueParameter>,
        ): List<IrValueParameter> {
            val boundIndices = buildMap {
                for ((index, param) in captured.withIndex()) {
                    if (param.correspondingParameter != null) {
                        put(param.correspondingParameter, index)
                    }
                }
            }

            return buildList {
                var unboundIndex = captured.size
                for (parameter in referencedFunctionParameters) {
                    val index = boundIndices[parameter] ?: unboundIndex++
                    add(wrapperFunctionParameters[index])
                }
                require(unboundIndex == wrapperFunctionParameters.size) {
                    "Wrong number of unbound parameters in wrapper: expected:${unboundIndex - captured.size} unbound, but ${wrapperFunctionParameters.size - captured.size} found"
                }
            }
        }
    }

    protected open fun copyNecessaryAttributes(oldReference: IrFunctionReference, newReference: IrRichFunctionReference) {}
    protected open fun copyNecessaryAttributes(oldReference: IrPropertyReference, newReference: IrRichPropertyReference) {}
    protected open fun copyNecessaryAttributes(oldReference: IrLocalDelegatedPropertyReference, newReference: IrRichPropertyReference) {}
    protected open fun IrDeclaration.hasMissingObjectDispatchReceiver(): Boolean = false
}
