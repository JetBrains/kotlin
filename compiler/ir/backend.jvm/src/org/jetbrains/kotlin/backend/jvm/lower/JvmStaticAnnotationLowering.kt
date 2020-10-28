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
import org.jetbrains.kotlin.backend.jvm.ir.replaceThisByStaticReference
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
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
            declaration.declarations.transformInPlace {
                if (it !is IrSimpleFunction || !it.isJvmStaticFunction() ||
                    it.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER
                ) return@transformInPlace it
                val staticMethod = context.cachedDeclarations.getStaticMethod(it)
                if (staticMethod != null) {
                    parent.declarations += staticMethod
                    declaration.buildProxy(staticMethod, null)
                } else {
                    parent.declarations += parent.buildProxy(it, context.cachedDeclarations.getFieldForObjectInstance(declaration))
                    it
                }
            }
        } else if (declaration.isObject) {
            declaration.declarations.transformInPlace {
                if (it !is IrSimpleFunction || !it.isJvmStaticFunction()) return@transformInPlace it
                val receiver = it.dispatchReceiverParameter ?: return@transformInPlace it
                context.cachedDeclarations.getStaticMethod(it)?.apply {
                    body = it.body
                        ?.replaceThisByStaticReference(context.cachedDeclarations, declaration, receiver)
                        ?.move(it, this, symbol, it.explicitParameters.drop(1).zip(explicitParameters).toMap())
                } ?: it
            }
        }
        return declaration
    }

    private fun IrClass.buildProxy(target: IrSimpleFunction, receiver: IrField?) =
        factory.buildFun {
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
            parent = this@buildProxy
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

    private fun <T : IrMemberAccessExpression<IrFunctionSymbol>> T.transform(clone: T.(IrSimpleFunctionSymbol) -> T): IrExpression? {
        if (!symbol.owner.isJvmStaticFunction()) return null
        transformChildren(this@JvmStaticAnnotationLowering, null)
        val staticMethod = context.cachedDeclarations.getStaticMethod(symbol.owner as IrSimpleFunction) ?: return null
        val staticAccess = clone(staticMethod.symbol).apply {
            copyTypeAndValueArgumentsFrom(this@transform)
            dispatchReceiver = null
        }
        return if (dispatchReceiver == null) staticAccess else IrBlockImpl(startOffset, endOffset, type, origin).apply {
            // Old receiver has to be evaluated for its side effects. TODO: what side effects do singleton object references have?
            statements += dispatchReceiver!!.coerceToUnit(context.irBuiltIns)
            statements += staticAccess
        }
    }

    override fun visitCall(expression: IrCall): IrExpression =
        expression.transform { IrCallImpl(startOffset, endOffset, type, it, typeArgumentsCount, valueArgumentsCount, origin) }
            ?: super.visitCall(expression)

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression =
        expression.transform { IrFunctionReferenceImpl(startOffset, endOffset, type, it, typeArgumentsCount, valueArgumentsCount) }
            ?: super.visitFunctionReference(expression)
}

private fun IrDeclaration.isJvmStaticFunction(): Boolean =
    this is IrSimpleFunction && origin != JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS &&
            (hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) ||
                    correspondingPropertySymbol?.owner?.hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) == true)
