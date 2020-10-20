/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.copyCorrespondingPropertyFrom
import org.jetbrains.kotlin.backend.jvm.ir.isInCurrentModule
import org.jetbrains.kotlin.backend.jvm.ir.replaceThisByStaticReference
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME

internal val jvmStaticAnnotationPhase = makeIrFilePhase(
    ::JvmStaticAnnotationLowering,
    name = "JvmStaticAnnotation",
    description = "Handle JvmStatic annotations"
)

/*
 * For @JvmStatic functions within companion objects of classes, we synthesize proxy static functions that redirect
 * to the actual implementation.
 * For @JvmStatic functions within static objects, we make the actual function static and modify all call sites.
 */
private class JvmStaticAnnotationLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildren(this, null)
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        declaration.transformChildren(this, null)
        if (declaration.isCompanion) {
            // Leave the functions as-is, but add static bridges to the parent class.
            val parent = declaration.parent as IrClass
            val needBridges = declaration.declarations.filter {
                it.isJvmStaticFunction() && it.origin != IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER
            }
            for (member in needBridges) {
                val function = member as IrSimpleFunction
                if (function.isExternal) {
                    // External functions are inverted, i.e. the actual code is in the static method of the parent class.
                    val staticMethod = function.copyRemovingDispatchReceiver(parent)
                    parent.declarations += staticMethod
                    // TODO: calls pointing to `function` will reach the proxy at runtime - should the IR be remapped?
                    declaration.declarations.remove(function)
                    declaration.addProxy(staticMethod, null)
                } else {
                    parent.addProxy(function, context.cachedDeclarations.getFieldForObjectInstance(declaration))
                }
            }
        } else if (declaration.isObject) {
            for (member in declaration.declarations) {
                if (!member.isJvmStaticFunction()) continue
                val function = member as IrSimpleFunction
                val receiver = function.dispatchReceiverParameter ?: continue
                function.dispatchReceiverParameter = null
                function.body = function.body?.replaceThisByStaticReference(context.cachedDeclarations, declaration, receiver)
            }
        }
        return declaration
    }

    private fun IrClass.addProxy(target: IrSimpleFunction, receiver: IrField?) =
        addFunction {
            returnType = target.returnType
            origin = JvmLoweredDeclarationOrigin.JVM_STATIC_WRAPPER
            // The proxy needs to have the same name as what it is targeting. If that is a property accessor,
            // we need to make sure that the name is mapped correctly. The static method is not a property accessor,
            // so we do not have a property to link it up to. Therefore, we compute the right name now.
            name = Name.identifier(context.methodSignatureMapper.mapFunctionName(target))
            modality = if (isInterface) Modality.OPEN else target.modality
            // Since we already mangle the name above we need to reset internal visibilities to public in order
            // to avoid mangling the same name twice.
            visibility = if (target.visibility == DescriptorVisibilities.INTERNAL) DescriptorVisibilities.PUBLIC else target.visibility
            isSuspend = target.isSuspend
        }.apply {
            copyAnnotationsFrom(target)
            copyParameterDeclarationsFrom(target)
            dispatchReceiverParameter = if (receiver == null)
                thisReceiver?.copyTo(this) // non-static proxy to static member
            else
                null // static proxy to non-static member
            body = context.createIrBuilder(symbol).run {
                irExprBody(irCall(target).apply {
                    for ((i, param) in typeParameters.withIndex()) {
                        putTypeArgument(i, param.defaultType)
                    }
                    dispatchReceiver = receiver?.let { irGetField(null, it) }
                    extensionReceiver = extensionReceiverParameter?.let { irGet(it) }
                    for ((i, valueParameter) in valueParameters.withIndex()) {
                        putValueArgument(i, irGet(valueParameter))
                    }
                })
            }
        }

    override fun visitCall(expression: IrCall): IrExpression {
        val callee = expression.symbol.owner
        if (callee.isJvmStaticInSingleton() && expression.dispatchReceiver != null) {
            return IrBlockImpl(expression.startOffset, expression.endOffset, expression.type, expression.origin).apply {
                // Old receiver has to be evaluated for its side effects. TODO: what side effects do singleton object references have?
                statements += super.visitExpression(expression.dispatchReceiver!!).coerceToUnit()
                statements += super.visitCall(if (callee.isInCurrentModule()) {
                    expression
                } else {
                    // Imported functions do not have their receiver parameter nulled by the code above, so we have to do it here.
                    // TODO: would be better handled by lowering imported declarations; or at least cache the stubs.
                    irCall(expression, callee.copyRemovingDispatchReceiver(callee.parent))
                }.apply { dispatchReceiver = null })
            }
        }
        return super.visitCall(expression)
    }

    private fun IrExpression.coerceToUnit(): IrExpression = IrTypeOperatorCallImpl(
        startOffset, endOffset, context.irBuiltIns.unitType, IrTypeOperator.IMPLICIT_COERCION_TO_UNIT, context.irBuiltIns.unitType, this
    )

    private fun IrSimpleFunction.copyRemovingDispatchReceiver(parent: IrDeclarationParent): IrSimpleFunction =
        factory.buildFun {
            updateFrom(this@copyRemovingDispatchReceiver)
            name = this@copyRemovingDispatchReceiver.name
            returnType = this@copyRemovingDispatchReceiver.returnType
        }.also {
            it.parent = parent
            it.copyAttributes(this)
            it.copyAnnotationsFrom(this)
            it.copyCorrespondingPropertyFrom(this)
            it.copyParameterDeclarationsFrom(this)
            it.dispatchReceiverParameter = null
        }
}

private fun IrDeclaration.isJvmStaticFunction(): Boolean =
    this is IrSimpleFunction && origin != JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS &&
            (hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) ||
                    correspondingPropertySymbol?.owner?.hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) == true)

private fun IrDeclaration.isJvmStaticInSingleton(): Boolean {
    val parentClass = parent as? IrClass ?: return false
    return isJvmStaticFunction() && parentClass.isObject && !parentClass.isCompanion
}
