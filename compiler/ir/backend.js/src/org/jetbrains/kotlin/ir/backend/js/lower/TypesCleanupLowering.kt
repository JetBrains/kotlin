package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.utils.isUnitInstanceFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class TypesCleanupLowering(val context: JsIrBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(TypesCleanupTransformer(context))
    }
}

class ExportedDeclarationsCleanupLowering(val context: JsIrBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration.isExported(context) && declaration is IrTypeParametersContainer) {
            declaration.typeParameters = emptyList()

            if (declaration is IrFunction) {
                declaration.returnType = context.irBuiltIns.anyNType
                declaration.valueParameters.forEach {
                    it.type = context.irBuiltIns.anyNType
                    it.varargElementType = null
                }
            }
        }
        if (declaration is IrField && declaration.correspondingPropertySymbol?.owner?.isExported(context) == true) {
            declaration.type = context.irBuiltIns.anyNType
        }
        return null
    }
}

private class TypesCleanupTransformer(val context: JsIrBackendContext) : IrElementTransformerVoid() {
    private fun IrSymbol.isDependentOnTypeParameterIntrinsic(): Boolean {
        return this === context.intrinsics.jsObjectCreateSymbol ||
                this === context.intrinsics.jsClass ||
                this === context.intrinsics.jsBoxIntrinsic ||
                this === context.intrinsics.jsUnboxIntrinsic
    }

    private fun IrSymbol.isSuspendIntrinsic(): Boolean {
        return this === context.intrinsics.jsInvokeSuspendSuperType ||
                this === context.intrinsics.jsInvokeSuspendSuperTypeWithReceiver ||
                this === context.intrinsics.jsInvokeSuspendSuperTypeWithReceiverAndParam
    }

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        if (!declaration.isExported(context) && declaration is IrTypeParametersContainer) {
            declaration.typeParameters = emptyList()
        }
        return super.visitDeclaration(declaration)
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        if (!declaration.isExported(context) && !declaration.symbol.isUnitInstanceFunction(context)) {
            declaration.returnType = context.irBuiltIns.anyNType
            declaration.valueParameters.forEach {
                it.type = context.irBuiltIns.anyNType
                it.varargElementType = null
            }
        }
        declaration.dispatchReceiverParameter?.type = context.irBuiltIns.anyNType
        declaration.extensionReceiverParameter?.type = context.irBuiltIns.anyNType
        return super.visitFunction(declaration)
    }

    override fun visitField(declaration: IrField): IrStatement {
        if (declaration.correspondingPropertySymbol?.owner?.isExported(context) != true) {
            declaration.type = context.irBuiltIns.anyNType
        }
        return super.visitField(declaration)
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        declaration.thisReceiver?.type = context.irBuiltIns.anyNType
        return super.visitClass(declaration)
    }

    override fun visitExpression(expression: IrExpression): IrExpression {
        if (expression.type != context.irBuiltIns.unitType && expression.type != context.irBuiltIns.stringType) {
            expression.type = context.irBuiltIns.anyNType
        }
        return super.visitExpression(expression)
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>): IrExpression {
        if (!expression.symbol.isDependentOnTypeParameterIntrinsic()) {
            expression.cleanTypeArguments()
        }
        val extensionReceiverType = expression.extensionReceiver?.type
        return super.visitMemberAccess(expression).apply {
            if (this is IrMemberAccessExpression<*> && symbol.isSuspendIntrinsic()) {
                extensionReceiverType?.let { extensionReceiver?.type = it }
            }
        }
    }
}