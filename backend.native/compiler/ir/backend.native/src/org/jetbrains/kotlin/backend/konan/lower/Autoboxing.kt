/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.AbstractValueUsageTransformer
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedPropertyDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * Boxes and unboxes values of value types when necessary.
 */
internal class Autoboxing(val context: Context) : FileLoweringPass {

    private val transformer = AutoboxingTransformer(context)

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(transformer)
        irFile.transform(InlineClassTransformer(context), data = null)
    }

}

private class AutoboxingTransformer(val context: Context) : AbstractValueUsageTransformer(
        context.builtIns,
        context.ir.symbols,
        context.irBuiltIns
) {

    // TODO: should we handle the cases when expression type
    // is not equal to e.g. called function return type?

    override fun IrExpression.useInTypeOperator(operator: IrTypeOperator, typeOperand: IrType): IrExpression {
        return if (operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT ||
                   operator == IrTypeOperator.IMPLICIT_INTEGER_COERCION) {
            this
        } else {
            // Codegen expects the argument of type-checking operator to be an object reference:
            this.useAs(context.irBuiltIns.anyNType)
        }
    }

    private var currentFunction: IrFunction? = null

    override fun visitFunction(declaration: IrFunction): IrStatement {
        currentFunction = declaration
        val result = super.visitFunction(declaration)
        currentFunction = null
        return result
    }

    override fun IrExpression.useAsReturnValue(returnTarget: IrReturnTargetSymbol): IrExpression = when (returnTarget) {
        is IrSimpleFunctionSymbol -> if (returnTarget.owner.isSuspend && returnTarget == currentFunction?.symbol) {
            this.useAs(irBuiltIns.anyNType)
        } else {
            this.useAs(returnTarget.owner.returnType)
        }
        is IrConstructorSymbol -> this.useAs(irBuiltIns.unitType)
        is IrReturnableBlockSymbol -> this.useAs(returnTarget.owner.type)
        else -> error(returnTarget)
    }

    override fun IrExpression.useAs(type: IrType): IrExpression {
        if (this.isNullConst() && type.isNullablePointer()) {
            // TODO: consider using IrConst with proper type.
            return IrCallImpl(
                    startOffset,
                    endOffset,
                    symbols.getNativeNullPtr.owner.returnType,
                    symbols.getNativeNullPtr
            ).uncheckedCast(type)
        }

        val actualType = when (this) {
            is IrCall -> {
                if (this.symbol.owner.isSuspend) irBuiltIns.anyNType
                else if (this.symbol == symbols.reinterpret) this.getTypeArgument(1)!!
                else this.callTarget.returnType
            }
            is IrGetField -> this.symbol.owner.type

            is IrTypeOperatorCall -> when (this.operator) {
                IrTypeOperator.IMPLICIT_INTEGER_COERCION ->
                    // TODO: is it a workaround for inconsistent IR?
                    this.typeOperand

                IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST -> context.irBuiltIns.anyNType

                else -> this.type
            }

            else -> this.type
        }

        return this.adaptIfNecessary(actualType, type)
    }

    private fun IrType.isNullablePointer(): Boolean =
            this.containsNull() && this.computePrimitiveBinaryTypeOrNull() == PrimitiveBinaryType.POINTER

    private val IrFunctionAccessExpression.target: IrFunction get() = when (this) {
        is IrCall -> this.callTarget
        is IrDelegatingConstructorCall -> this.symbol.owner
        is IrConstructorCall -> this.symbol.owner
        else -> TODO(this.render())
    }

    private val IrCall.callTarget: IrFunction
        get() = if (superQualifierSymbol == null && symbol.owner.isOverridable) {
            // A virtual call.
            symbol.owner
        } else {
            symbol.owner.target
        }

    override fun IrExpression.useAsDispatchReceiver(expression: IrFunctionAccessExpression): IrExpression {
        return this.useAsArgument(expression.target.dispatchReceiverParameter!!)
    }

    override fun IrExpression.useAsExtensionReceiver(expression: IrFunctionAccessExpression): IrExpression {
        return this.useAsArgument(expression.target.extensionReceiverParameter!!)
    }

    override fun IrExpression.useAsValueArgument(expression: IrFunctionAccessExpression,
                                                 parameter: IrValueParameter): IrExpression {

        return this.useAsArgument(expression.target.valueParameters[parameter.index])
    }

    private fun IrExpression.adaptIfNecessary(actualType: IrType, expectedType: IrType): IrExpression {
        val conversion = symbols.getTypeConversion(actualType, expectedType)
        return if (conversion == null) {
            this
        } else {
            val parameter = conversion.owner.explicitParameters.single()
            val argument = this.uncheckedCast(parameter.type)

            IrCallImpl(startOffset, endOffset, conversion.owner.returnType, conversion).apply {
                addArguments(mapOf(parameter to argument))
            }.uncheckedCast(this.type) // Try not to bring new type incompatibilities.
        }
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        expression.transformChildrenVoid()
        assert(expression.getArgumentsWithIr().isEmpty())
        return expression
    }

    /**
     * Casts this expression to `type` without changing its representation in generated code.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun IrExpression.uncheckedCast(type: IrType): IrExpression {
        // TODO: apply some cast if types are incompatible; not required currently.
        return this
    }

    override fun visitCall(expression: IrCall): IrExpression {
        return when (expression.symbol) {
            symbols.reinterpret -> {
                expression.transformChildrenVoid()

                // TODO: check types has the same binary representation.
                val oldType = expression.getTypeArgument(0)!!
                val newType = expression.getTypeArgument(1)!!

                assert(oldType.computePrimitiveBinaryTypeOrNull() == newType.computePrimitiveBinaryTypeOrNull())

                expression.extensionReceiver = expression.extensionReceiver!!.useAs(oldType)

                expression
            }

            else -> super.visitCall(expression)
        }
    }

}

private class InlineClassTransformer(private val context: Context) : IrBuildingTransformer(context) {

    private val symbols = context.ir.symbols
    private val irBuiltIns = context.irBuiltIns

    private val builtBoxUnboxFunctions = mutableListOf<IrFunction>()

    override fun visitFile(declaration: IrFile): IrFile {
        declaration.transformChildrenVoid(this)
        declaration.declarations.addAll(builtBoxUnboxFunctions)
        builtBoxUnboxFunctions.clear()
        return declaration
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        super.visitClass(declaration)

        if (declaration.isInlined()) {
            if (declaration.isUsedAsBoxClass()) {
                if (KonanPrimitiveType.byFqName[declaration.fqNameForIrSerialization.toUnsafe()] != null) {
                    buildBoxField(declaration)
                }

                buildBoxFunction(declaration, context.getBoxFunction(declaration))
                buildUnboxFunction(declaration, context.getUnboxFunction(declaration))
            }

            declaration.constructors.filter { !it.isPrimary }.toList().mapTo(declaration.declarations) {
                context.getLoweredInlineClassConstructor(it)
            }
        }

        return declaration
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        super.visitGetField(expression)

        val field = expression.symbol.owner
        val parentClass = field.parentClassOrNull
        return if (parentClass == null || !parentClass.isInlined())
            expression
        else {
            builder.at(expression)
                    .irCall(symbols.reinterpret, field.type,
                            listOf(parentClass.defaultType, field.type)
                    ).apply {
                        extensionReceiver = expression.receiver!!
                    }
        }
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        super.visitSetField(expression)

        return if (expression.symbol.owner.parentClassOrNull?.isInlined() == true) {
            // TODO: it is better to get rid of functions setting such fields.
            // Here we're trying to maintain all IR nodes as is, albeit the transformed IR isn't equivalent to the original.
            // By far SET_FIELD can only be in the constructor which won't be codegened.
            // Box functions use createUninitializedInstance instead of constructor calls
            // and are placed separately so they won't be processed here.
            val startOffset = expression.startOffset
            val endOffset = expression.endOffset
            IrBlockImpl(startOffset, endOffset, irBuiltIns.unitType).apply {
                statements.addIfNotNull(expression.receiver)
                statements += expression.value
                statements += IrCallImpl(startOffset, endOffset, irBuiltIns.nothingType, symbols.throwNullPointerException)
                statements += IrGetObjectValueImpl(startOffset, endOffset, irBuiltIns.unitType, irBuiltIns.unitClass)
            }
        } else {
            expression
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        super.visitConstructorCall(expression)

        val constructor = expression.symbol.owner
        return if (constructor.constructedClass.isInlined()) {
            builder.lowerConstructorCallToValue(expression, constructor)
        } else {
            expression
        }
    }

    override fun visitConstructor(declaration: IrConstructor): IrStatement {
        super.visitConstructor(declaration)

        if (declaration.constructedClass.isInlined()) {
            if (!declaration.isPrimary) {
                buildLoweredSecondaryConstructor(declaration)
            }
            // TODO: fix DFG building and nullify the body instead.
            (declaration.body as IrBlockBody).statements.clear()
        }

        return declaration
    }

    private fun IrBuilderWithScope.irIsNull(expression: IrExpression): IrExpression {
        val binary = expression.type.computeBinaryType()
        return when (binary) {
            is BinaryType.Primitive -> {
                assert(binary.type == PrimitiveBinaryType.POINTER)
                irCall(symbols.areEqualByValue[binary.type]!!.owner).apply {
                    putValueArgument(0, expression)
                    putValueArgument(1, irNullPointer())
                }
            }
            is BinaryType.Reference -> irCall(context.irBuiltIns.eqeqeqSymbol).apply {
                putValueArgument(0, expression)
                putValueArgument(1, irNull())
            }
        }
    }

    private fun buildBoxFunction(irClass: IrClass, function: IrFunction) {
        val builder = context.createIrBuilder(function.symbol)
        val cache = BoxCache.values().toList().atMostOne { context.irBuiltIns.getKotlinClass(it) == irClass }

        function.body = builder.irBlockBody(function) {
            val valueToBox = function.valueParameters[0]
            if (valueToBox.type.containsNull()) {
                +irIfThen(
                        condition = irIsNull(irGet(valueToBox)),
                        thenPart = irReturn(irNull())
                )
            }

            if (cache != null) {
                +irIfThen(
                        condition = irCall(symbols.boxCachePredicates[cache]!!.owner).apply {
                            putValueArgument(0, irGet(valueToBox))
                        },
                        thenPart = irReturn(irCall(symbols.boxCacheGetters[cache]!!.owner).apply {
                            putValueArgument(0, irGet(valueToBox))
                        })
                )
            }

            // Note: IR variable created below has reference type intentionally.
            val box = irTemporary(irCall(symbols.createUninitializedInstance.owner).also {
                it.putTypeArgument(0, irClass.defaultType)
            })
            +irSetField(irGet(box), getInlineClassBackingField(irClass), irGet(valueToBox))
            +irReturn(irGet(box))
        }

        builtBoxUnboxFunctions += function
    }

    private fun IrBuilderWithScope.irNullPointerOrReference(type: IrType): IrExpression =
            if (type.binaryTypeIsReference()) {
                irNull()
            } else {
                irNullPointer()
            }

    private fun IrBuilderWithScope.irNullPointer(): IrExpression = irCall(symbols.getNativeNullPtr.owner)

    private fun buildUnboxFunction(irClass: IrClass, function: IrFunction) {
        val builder = context.createIrBuilder(function.symbol)

        function.body = builder.irBlockBody(function) {
            val boxParameter = function.valueParameters.single()
            if (boxParameter.type.containsNull()) {
                +irIfThen(
                        condition = irEqeqeq(irGet(boxParameter), irNull()),
                        thenPart = irReturn(irNullPointerOrReference(function.returnType))
                )
            }
            +irReturn(irGetField(irGet(boxParameter), getInlineClassBackingField(irClass)))
        }

        builtBoxUnboxFunctions += function
    }

    private fun buildBoxField(declaration: IrClass) {
        val startOffset = declaration.startOffset
        val endOffset = declaration.endOffset
        val descriptor = WrappedPropertyDescriptor()

        val irField = IrFieldImpl(
                startOffset,
                endOffset,
                IrDeclarationOrigin.DEFINED,
                IrFieldSymbolImpl(descriptor),
                Name.identifier("value"),
                declaration.defaultType,
                Visibilities.PRIVATE,
                isFinal = true,
                isExternal = false,
                isStatic = false,
        )
        irField.parent = declaration

        val irProperty = IrPropertyImpl(
                startOffset,
                endOffset,
                IrDeclarationOrigin.DEFINED,
                IrPropertySymbolImpl(descriptor),
                irField.name,
                irField.visibility,
                Modality.FINAL,
                isVar = false,
                isConst = false,
                isLateinit = false,
                isDelegated = false,
                isExternal = false
        )
        descriptor.bind(irProperty)
        irProperty.backingField = irField

        declaration.addChild(irProperty)
    }

    private fun IrBuilderWithScope.lowerConstructorCallToValue(
            expression: IrMemberAccessExpression<*>,
            callee: IrConstructor
    ): IrExpression = if (callee.isPrimary) {
        expression.getValueArgument(0)!!
    } else {
        this.at(expression).irCall(this@InlineClassTransformer.context.getLoweredInlineClassConstructor(callee)).apply {
            (0 until expression.valueArgumentsCount).forEach {
                putValueArgument(it, expression.getValueArgument(it)!!)
            }
        }
    }

    private fun buildLoweredSecondaryConstructor(irConstructor: IrConstructor) {
        val result = context.getLoweredInlineClassConstructor(irConstructor)
        val irClass = irConstructor.parentAsClass

        result.body = context.createIrBuilder(result.symbol).irBlockBody(result) {
            lateinit var thisVar: IrVariable
            val parameterMapping = result.valueParameters.associateBy {
                irConstructor.valueParameters[it.index].symbol
            }

            (irConstructor.body as IrBlockBody).statements.forEach { statement ->
                statement.setDeclarationsParent(result)
                +statement.transform(object : IrElementTransformerVoid() {
                    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                        expression.transformChildrenVoid()

                        val value = lowerConstructorCallToValue(expression, expression.symbol.owner)
                        return irBlock(expression) {
                            thisVar = irTemporary(value)
                        }
                    }

                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        expression.transformChildrenVoid()
                        if (expression.symbol == irClass.thisReceiver?.symbol) {
                            return irGet(thisVar)
                        }

                        parameterMapping[expression.symbol]?.let { return irGet(it) }
                        return expression
                    }

                    override fun visitReturn(expression: IrReturn): IrExpression {
                        expression.transformChildrenVoid()
                        if (expression.returnTargetSymbol == irConstructor.symbol) {
                            return irReturn(irBlock(expression.startOffset, expression.endOffset) {
                                +expression.value
                                +irGet(thisVar)
                            })
                        }

                        return expression
                    }

                }, null)
            }
            +irReturn(irGet(thisVar))
        }
    }

    private fun getInlineClassBackingField(irClass: IrClass): IrField =
            irClass.declarations.filterIsInstance<IrProperty>().mapNotNull { it.backingField }.single()
}

private val Context.getLoweredInlineClassConstructor: (IrConstructor) -> IrSimpleFunction by Context.lazyMapMember { irConstructor ->
    require(irConstructor.constructedClass.isInlined())
    require(!irConstructor.isPrimary)

    val descriptor = WrappedSimpleFunctionDescriptor(irConstructor.descriptor.annotations, irConstructor.descriptor.source)
    IrFunctionImpl(
            irConstructor.startOffset, irConstructor.endOffset,
            IrDeclarationOrigin.DEFINED,
            IrSimpleFunctionSymbolImpl(descriptor),
            Name.special("<constructor>"),
            irConstructor.visibility,
            Modality.FINAL,
            isInline = false,
            isExternal = false,
            isTailrec = false,
            isSuspend = false,
            returnType = irConstructor.returnType,
            isExpect = false,
            isFakeOverride = false,
            isOperator = false,
            isInfix = false
    ).apply {
        descriptor.bind(this)
        parent = irConstructor.parent
        valueParameters += irConstructor.valueParameters.map { it.copyTo(this) }
    }
}