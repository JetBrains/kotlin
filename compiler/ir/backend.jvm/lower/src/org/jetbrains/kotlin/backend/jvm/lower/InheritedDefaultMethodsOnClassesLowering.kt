/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.deserialization.PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.expressions.putArgument
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Adds bridge implementations in classes that inherit default implementations from interfaces.
 */
@PhaseDescription(name = "InheritedDefaultMethodsOnClasses")
internal class InheritedDefaultMethodsOnClassesLowering(val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (!irClass.isJvmInterface) {
            irClass.declarations.transformInPlace {
                transformMemberDeclaration(it)
            }
        }
    }

    private fun transformMemberDeclaration(declaration: IrDeclaration): IrDeclaration {
        if (declaration !is IrSimpleFunction) return declaration

        if (declaration.isFakeOverride && declaration.name.asString() == "clone") {
            val overriddenFunctions = declaration.allOverridden(false)
            val cloneFun = overriddenFunctions.find { it.parentAsClass.hasEqualFqName(StandardNames.FqNames.cloneable.toSafe()) }
            if (cloneFun != null && overriddenFunctions.all { it.isFakeOverride || it == cloneFun }) {
                return generateCloneImplementation(declaration, cloneFun)
            }
        }

        val implementation = declaration.findInterfaceImplementation(context.config.jvmDefaultMode)
            ?: return declaration
        return generateDelegationToDefaultImpl(implementation, declaration)
    }

    private fun generateCloneImplementation(fakeOverride: IrSimpleFunction, cloneFun: IrSimpleFunction): IrSimpleFunction {
        assert(fakeOverride.isFakeOverride)
        val irFunction = context.cachedDeclarations.getDefaultImplsRedirection(fakeOverride)
        val irClass = fakeOverride.parentAsClass
        val classStartOffset = irClass.startOffset
        context.createJvmIrBuilder(irFunction.symbol, classStartOffset, classStartOffset).apply {
            irFunction.body = irBlockBody {
                +irReturn(
                    irCall(cloneFun, origin = null, superQualifierSymbol = cloneFun.parentAsClass.symbol).apply {
                        dispatchReceiver = irGet(irFunction.dispatchReceiverParameter!!)
                    }
                )
            }
        }
        return irFunction
    }

    private fun generateDelegationToDefaultImpl(
        interfaceImplementation: IrSimpleFunction,
        classOverride: IrSimpleFunction
    ): IrSimpleFunction {
        val irFunction = context.cachedDeclarations.getDefaultImplsRedirection(classOverride)

        val superMethod = firstSuperMethodFromKotlin(irFunction, interfaceImplementation).owner
        val superClassType = superMethod.parentAsClass.defaultType
        val defaultImplFun = context.cachedDeclarations.getDefaultImplsFunction(superMethod)
        val classStartOffset = classOverride.parentAsClass.startOffset
        val backendContext = context
        context.createIrBuilder(irFunction.symbol, classStartOffset, classStartOffset).apply {
            irFunction.body = irExprBody(irBlock {
                val parameter2arguments = backendContext.multiFieldValueClassReplacements
                    .mapFunctionMfvcStructures(this, defaultImplFun, irFunction) { sourceParameter, _ ->
                        irGet(sourceParameter).let {
                            if (sourceParameter != irFunction.dispatchReceiverParameter) it
                            else it.reinterpretAsDispatchReceiverOfType(superClassType)
                        }
                    }
                +irCall(defaultImplFun.symbol, irFunction.returnType).apply {
                    for (index in superMethod.parentAsClass.typeParameters.indices) {
                        putTypeArgument(index, createPlaceholderAnyNType(context.irBuiltIns))
                    }
                    passTypeArgumentsFrom(irFunction, offset = superMethod.parentAsClass.typeParameters.size)

                    for ((parameter, argument) in parameter2arguments) {
                        if (argument != null) {
                            putArgument(parameter, argument)
                        }
                    }
                }
            })
        }

        return irFunction
    }
}

/**
 * Redirects super interface calls to DefaultImpls.
 */
@PhaseDescription(name = "InterfaceSuperCalls")
internal class InterfaceSuperCallsLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val superQualifierClass = expression.superQualifierSymbol?.owner
        if (superQualifierClass == null || !superQualifierClass.isInterface || expression.isSuperToAny()) {
            return super.visitCall(expression)
        }

        val superCallee = expression.symbol.owner
        if (superCallee.isDefinitelyNotDefaultImplsMethod(context.config.jvmDefaultMode, superCallee.resolveFakeOverride()))
            return super.visitCall(expression)

        val redirectTarget = context.cachedDeclarations.getDefaultImplsFunction(superCallee)
        val newCall = createDelegatingCallWithPlaceholderTypeArguments(expression, redirectTarget, context.irBuiltIns)
        postprocessMovedThis(newCall)
        return super.visitCall(newCall)
    }

    private fun postprocessMovedThis(irCall: IrCall) {
        val movedThisParameter = irCall.symbol.owner.valueParameters
            .find { it.origin == IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER }
            ?: return
        val movedThisParameterIndex = movedThisParameter.indexInOldValueParameters
        irCall.putValueArgument(
            movedThisParameterIndex,
            irCall.getValueArgument(movedThisParameterIndex)?.reinterpretAsDispatchReceiverOfType(movedThisParameter.type)
        )
    }
}

// Given a dispatch receiver expression, wrap it in REINTERPRET_CAST to the given type,
// unless it's a value of inline class (which could be boxed at this point).
// Avoids a CHECKCAST on a moved dispatch receiver argument.
internal fun IrExpression.reinterpretAsDispatchReceiverOfType(irType: IrType): IrExpression =
    if (this.type.isInlineClassType())
        this
    else
        IrTypeOperatorCallImpl(
            this.startOffset, this.endOffset,
            irType, IrTypeOperator.REINTERPRET_CAST, irType,
            this
        )

/**
 * Redirects interface calls with default arguments to DefaultImpls (except methods compiled to JVM defaults).
 */
@PhaseDescription(name = "InterfaceDefaultCalls")
internal class InterfaceDefaultCallsLowering(val context: JvmBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {
    // TODO If there are no default _implementations_ we can avoid generating defaultImpls class entirely by moving default arg dispatchers to the interface class
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val callee = expression.symbol.owner

        if (!callee.hasInterfaceParent() ||
            callee.origin != IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER ||
            callee.isSimpleFunctionCompiledToJvmDefault(context.config.jvmDefaultMode)
        ) {
            return super.visitCall(expression)
        }

        val redirectTarget = context.cachedDeclarations.getDefaultImplsFunction(callee)

        // InterfaceLowering bridges from DefaultImpls in compatibility mode -- if that's the case,
        // this phase will inadvertently cause a recursive loop as the bridge on the DefaultImpls
        // gets redirected to call itself.
        if (redirectTarget == currentFunction?.irElement) return super.visitCall(expression)

        val newCall = createDelegatingCallWithPlaceholderTypeArguments(expression, redirectTarget, context.irBuiltIns)

        return super.visitCall(newCall)
    }
}

internal fun IrSimpleFunction.isDefinitelyNotDefaultImplsMethod(
    jvmDefaultMode: JvmDefaultMode,
    implementation: IrSimpleFunction?,
): Boolean =
    implementation == null ||
            implementation.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB ||
            implementation.isCompiledToJvmDefault(jvmDefaultMode) ||
            origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER ||
            hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME) ||
            isCloneableClone()

private fun IrSimpleFunction.isCloneableClone(): Boolean =
    name.asString() == "clone" &&
            (parent as? IrClass)?.fqNameWhenAvailable?.asString() == "kotlin.Cloneable" &&
            valueParameters.isEmpty()

/**
 * Resolves calls to Object methods on interface types to virtual methods.
 */
@PhaseDescription(name = "InterfaceObjectCalls")
internal class InterfaceObjectCallsLowering(val context: JvmBackendContext) : IrElementVisitorVoid, FileLoweringPass {
    override fun lower(irFile: IrFile) = irFile.acceptChildren(this, null)

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitCall(expression: IrCall) {
        expression.acceptChildren(this, null)

        if (expression.superQualifierSymbol != null && !expression.isSuperToAny()) return

        val callee = expression.symbol.owner
        if (!callee.isMethodOfAny()) return
        if (!callee.hasInterfaceParent() && expression.dispatchReceiver?.run { type.erasedUpperBound.isJvmInterface } != true) return

        val resolved = callee.resolveFakeOverride() ?: return
        expression.symbol = resolved.symbol
        if (expression.superQualifierSymbol != null) {
            expression.superQualifierSymbol = context.irBuiltIns.anyClass
        }
    }
}

/**
 * Given a fake override in a class, returns an overridden declaration with implementation in interface, such that a method delegating to that
 * interface implementation should be generated into the class containing the fake override; or null if the given function is not a fake
 * override of any interface implementation or such method was already generated into the superclass or is a method from Any.
 */
internal fun IrSimpleFunction.findInterfaceImplementation(
    jvmDefaultMode: JvmDefaultMode, allowJvmDefault: Boolean = false,
): IrSimpleFunction? {
    if (!isFakeOverride) return null

    val parent = parent
    if (parent is IrClass && (parent.isJvmInterface || parent.isFromJava())) return null

    val implementation = resolveFakeOverride(toSkip = ::isDefaultImplsBridge) ?: return null

    if (!implementation.hasInterfaceParent()
        || DescriptorVisibilities.isPrivate(implementation.visibility)
        || implementation.isMethodOfAny()
    ) {
        return null
    }

    if (!allowJvmDefault && implementation.isDefinitelyNotDefaultImplsMethod(jvmDefaultMode, implementation)) return null

    // Only generate interface delegation for functions immediately inherited from an interface.
    // (Otherwise, delegation will be present in the parent class)
    if (overriddenSymbols.any {
            !it.owner.parentAsClass.isInterface &&
                    it.owner.modality != Modality.ABSTRACT &&
                    it.owner.resolveFakeOverride(toSkip = ::isDefaultImplsBridge) == implementation
        }) {
        return null
    }

    return implementation
}

private fun isDefaultImplsBridge(f: IrSimpleFunction) =
    f.origin == JvmLoweredDeclarationOrigin.SUPER_INTERFACE_METHOD_BRIDGE
