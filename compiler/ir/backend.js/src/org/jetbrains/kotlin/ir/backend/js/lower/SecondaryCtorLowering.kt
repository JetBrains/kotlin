/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

data class ConstructorPair(val delegate: IrSimpleFunction, val stub: IrSimpleFunction)

class SecondaryConstructorLowering(val context: JsIrBackendContext) : ClassLoweringPass {

    private val oldCtorToNewMap = context.secondaryConstructorToFactoryCache

    override fun lower(irClass: IrClass) {
        if (irClass.isInline) return

        irClass.declarations.transformFlat {
            if (it is IrConstructor) {
                if (it.isPrimary) null else transformConstructor(it, irClass)
            } else null
        }
    }

    private fun transformConstructor(constructor: IrConstructor, irClass: IrClass): List<IrSimpleFunction> {
        val stubs = oldCtorToNewMap.getOrPut(constructor) {
            buildConstructorStubDeclarations(constructor, irClass)
        }

        generateStubsBody(constructor, irClass, stubs)

        return listOf(stubs.delegate, stubs.stub)
    }

    private fun generateStubsBody(constructor: IrConstructor, irClass: IrClass, stubs: ConstructorPair) {
        // We should split secondary constructor into two functions,
        //   *  Initializer which contains constructor's body and takes just created object as implicit param `$this`
        //   **   This function is also delegation constructor
        //   *  Creation function which has same signature with original constructor,
        //      creates new object via `Object.create` builtIn and passes it to corresponding `Init` function
        // In other words:
        // Foo::constructor(...) {
        //   body
        // }
        // =>
        // Foo_init_$Init$(..., $this) {
        //   body[ this = $this ]
        //   return $this
        // }
        // Foo_init_$Create$(...) {
        //   val t = Object.create(Foo.prototype);
        //   return Foo_init_$Init$(..., t)
        // }
        generateInitBody(constructor, irClass, stubs.delegate)
        generateFactoryBody(constructor, irClass, stubs.stub, stubs.delegate)
    }

    private fun generateFactoryBody(constructor: IrConstructor, irClass: IrClass, stub: IrSimpleFunction, delegate: IrSimpleFunction) {
        val type = irClass.defaultType
        val createFunctionIntrinsic = context.intrinsics.jsObjectCreate
        val irCreateCall = JsIrBuilder.buildCall(createFunctionIntrinsic.symbol, type, listOf(type))
        val irDelegateCall = JsIrBuilder.buildCall(delegate.symbol, type).also { call ->
            for (i in 0 until stub.typeParameters.size) {
                call.putTypeArgument(i, stub.typeParameters[i].toIrType())
            }

            for (i in 0 until stub.valueParameters.size) {
                call.putValueArgument(i, JsIrBuilder.buildGetValue(stub.valueParameters[i].symbol))
            }

            call.putValueArgument(constructor.valueParameters.size, irCreateCall)
        }
        val irReturn = JsIrBuilder.buildReturn(stub.symbol, irDelegateCall, context.irBuiltIns.nothingType)


        stub.body = JsIrBuilder.buildBlockBody(listOf(irReturn))
    }

    private fun generateInitBody(constructor: IrConstructor, irClass: IrClass, delegate: IrSimpleFunction) {
        val thisParam = delegate.valueParameters.last()
        val oldThisReceiver = irClass.thisReceiver!!
        val retStmt = JsIrBuilder.buildReturn(delegate.symbol, JsIrBuilder.buildGetValue(thisParam.symbol), context.irBuiltIns.nothingType)
        val statements = (constructor.body!!.deepCopyWithSymbols(delegate) as IrStatementContainer).statements

        val oldValueParameters = constructor.valueParameters + oldThisReceiver

        // TODO: replace parameters as well
        delegate.body = JsIrBuilder.buildBlockBody(statements + retStmt).apply {
            transformChildrenVoid(ThisUsageReplaceTransformer(delegate.symbol, oldValueParameters.zip(delegate.valueParameters).toMap()))
        }
    }


    private class ThisUsageReplaceTransformer(
        val function: IrFunctionSymbol,
        val symbolMapping: Map<IrValueParameter, IrValueParameter>
    ) : IrElementTransformerVoid() {

        val newThisSymbol = symbolMapping.values.last().symbol

        override fun visitReturn(expression: IrReturn): IrExpression = IrReturnImpl(
            expression.startOffset,
            expression.endOffset,
            expression.type,
            function,
            IrGetValueImpl(expression.startOffset, expression.endOffset, newThisSymbol.owner.type, newThisSymbol)
        )

        override fun visitGetValue(expression: IrGetValue) = symbolMapping[expression.symbol.owner]?.let {
            expression.run { IrGetValueImpl(startOffset, endOffset, type, it.symbol, origin) }
        } ?: expression
    }
}

private fun IrTypeParameter.toIrType() = IrSimpleTypeImpl(symbol, true, emptyList(), emptyList())

private fun buildInitDeclaration(constructor: IrConstructor, irClass: IrClass): IrSimpleFunction {
    val type = irClass.defaultType
    val constructorName = "${irClass.name}_init"
    val functionName = "${constructorName}_\$Init\$"

    return JsIrBuilder.buildFunction(
        functionName,
        type,
        constructor.parent,
        constructor.visibility,
        Modality.FINAL,
        constructor.isInline,
        constructor.isExternal
    ).also {
        it.copyTypeParametersFrom(constructor.parentAsClass)

        constructor.valueParameters.mapTo(it.valueParameters) { p -> p.copyTo(it) }
        it.valueParameters += JsIrBuilder.buildValueParameter("\$this", constructor.valueParameters.size, type).apply { parent = it }
    }
}

private fun buildFactoryDeclaration(constructor: IrConstructor, irClass: IrClass): IrSimpleFunction {
    val type = irClass.defaultType
    val constructorName = "${irClass.name}_init"
    val functionName = "${constructorName}_\$Create\$"

    return JsIrBuilder.buildFunction(
        functionName,
        type,
        constructor.parent,
        constructor.visibility,
        Modality.FINAL,
        constructor.isInline,
        constructor.isExternal
    ).also {
        it.copyTypeParametersFrom(constructor.parentAsClass)
        it.valueParameters += constructor.valueParameters.map { p -> p.copyTo(it) }
    }
}

private fun buildConstructorStubDeclarations(constructor: IrConstructor, klass: IrClass) =
    ConstructorPair(buildInitDeclaration(constructor, klass), buildFactoryDeclaration(constructor, klass))

class SecondaryFactoryInjectorLowering(val context: JsIrBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(CallsiteRedirectionTransformer(context), null)
    }
}

private class CallsiteRedirectionTransformer(context: JsIrBackendContext) : IrElementTransformer<IrFunction?> {

    private val oldCtorToNewMap = context.secondaryConstructorToFactoryCache
    private val defaultThrowableConstructor = context.defaultThrowableCtor

    private val IrConstructor.isSecondaryConstructorCall
        get() =
            !isPrimary && this != defaultThrowableConstructor && !isExternal && !parentAsClass.isInline

    override fun visitFunction(declaration: IrFunction, data: IrFunction?): IrStatement = super.visitFunction(declaration, declaration)

    override fun visitConstructorCall(expression: IrConstructorCall, data: IrFunction?): IrElement {
        super.visitConstructorCall(expression, data)

        val target = expression.symbol.owner
        return if (target.isSecondaryConstructorCall) {
            val ctor = oldCtorToNewMap.getOrPut(target) {
                buildConstructorStubDeclarations(target, target.parentAsClass)
            }
            replaceSecondaryConstructorWithFactoryFunction(expression, ctor.stub.symbol)
        } else expression
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: IrFunction?): IrElement {
        super.visitDelegatingConstructorCall(expression, data)

        val target = expression.symbol.owner

        return if (target.isSecondaryConstructorCall) {
            val klass = target.parentAsClass
            val ctor = oldCtorToNewMap.getOrPut(target) { buildConstructorStubDeclarations(target, klass) }
            val newCall = replaceSecondaryConstructorWithFactoryFunction(expression, ctor.delegate.symbol)

            val readThis = expression.run {
                if (data!! is IrConstructor) {
                    val thisReceiver = klass.thisReceiver!!
                    IrGetValueImpl(startOffset, endOffset, thisReceiver.type, thisReceiver.symbol)
                } else {
                    val lastValueParameter = data.valueParameters.last()
                    IrGetValueImpl(startOffset, endOffset, lastValueParameter.type, lastValueParameter.symbol)
                }
            }

            newCall.apply { putValueArgument(expression.valueArgumentsCount, readThis) }
        } else expression
    }

    private fun replaceSecondaryConstructorWithFactoryFunction(
        call: IrFunctionAccessExpression,
        newTarget: IrSimpleFunctionSymbol
    ) = IrCallImpl(call.startOffset, call.endOffset, call.type, newTarget, newTarget.descriptor, call.typeArgumentsCount).apply {

        copyTypeArgumentsFrom(call)

        for (i in 0 until call.valueArgumentsCount) {
            putValueArgument(i, call.getValueArgument(i))
        }
    }
}