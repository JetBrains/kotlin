/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.deserialization.PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClass
import org.jetbrains.kotlin.ir.declarations.lazy.IrMaybeDeserializedClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_DEFAULT_FQ_NAME
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_DEFAULT_WITH_COMPATIBILITY_FQ_NAME

fun IrFunction.isSimpleFunctionCompiledToJvmDefault(jvmDefaultMode: JvmDefaultMode): Boolean {
    return (this as? IrSimpleFunction)?.isCompiledToJvmDefault(jvmDefaultMode) == true
}

fun IrSimpleFunction.isCompiledToJvmDefault(jvmDefaultMode: JvmDefaultMode): Boolean {
    assert(!isFakeOverride && parentAsClass.isInterface && modality != Modality.ABSTRACT) {
        "`isCompiledToJvmDefault` should be called on non-fakeoverrides and non-abstract methods from interfaces ${ir2string(this)}"
    }
    if (origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) return false
    if (hasJvmDefault()) return true
    when (val klass = parentAsClass) {
        is IrLazyClass -> klass.classProto?.let {
            return JvmProtoBufUtil.isNewPlaceForBodyGeneration(it)
        }
        is IrMaybeDeserializedClass -> return klass.isNewPlaceForBodyGeneration
    }
    return jvmDefaultMode.isEnabled
}

fun IrFunction.hasJvmDefault(): Boolean = propertyIfAccessor.hasAnnotation(JVM_DEFAULT_FQ_NAME)
fun IrClass.hasJvmDefaultNoCompatibilityAnnotation(): Boolean = hasAnnotation(JVM_DEFAULT_NO_COMPATIBILITY_FQ_NAME)
fun IrClass.hasJvmDefaultWithCompatibilityAnnotation(): Boolean = hasAnnotation(JVM_DEFAULT_WITH_COMPATIBILITY_FQ_NAME)
fun IrFunction.hasPlatformDependent(): Boolean = propertyIfAccessor.hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME)

fun IrSimpleFunction.isDefinitelyNotDefaultImplsMethod(
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
 * Given a fake override in a class, returns an overridden declaration with implementation in interface, such that a method delegating to that
 * interface implementation should be generated into the class containing the fake override; or null if the given function is not a fake
 * override of any interface implementation or such method was already generated into the superclass or is a method from Any.
 */
fun IrSimpleFunction.findInterfaceImplementation(jvmDefaultMode: JvmDefaultMode, allowJvmDefault: Boolean = false): IrSimpleFunction? {
    if (!isFakeOverride) return null

    val parent = parent
    if (parent is IrClass && (parent.isJvmInterface || parent.isFromJava())) return null

    val implementation = resolveFakeOverride(isFakeOverride = ::isFakeOverrideOrDefaultImplsBridge) ?: return null

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
                    it.owner.resolveFakeOverride(isFakeOverride = ::isFakeOverrideOrDefaultImplsBridge) == implementation
        }) {
        return null
    }

    return implementation
}

private fun isFakeOverrideOrDefaultImplsBridge(f: IrSimpleFunction): Boolean =
    f.isFakeOverride || f.origin == JvmLoweredDeclarationOrigin.SUPER_INTERFACE_METHOD_BRIDGE

fun IrFactory.createDefaultImplsRedirection(fakeOverride: IrSimpleFunction): IrSimpleFunction {
    assert(fakeOverride.isFakeOverride) { "Function should be a fake override: ${fakeOverride.render()}" }
    val irClass = fakeOverride.parentAsClass
    return buildFun {
        origin = JvmLoweredDeclarationOrigin.SUPER_INTERFACE_METHOD_BRIDGE
        name = fakeOverride.name
        visibility = fakeOverride.visibility
        modality = fakeOverride.modality
        returnType = fakeOverride.returnType
        isInline = fakeOverride.isInline
        isExternal = false
        isTailrec = false
        isSuspend = fakeOverride.isSuspend
        isOperator = fakeOverride.isOperator
        isInfix = fakeOverride.isInfix
        isExpect = false
        isFakeOverride = false
    }.apply {
        parent = irClass
        overriddenSymbols = fakeOverride.overriddenSymbols
        copyValueAndTypeParametersFrom(fakeOverride)
        // The fake override's dispatch receiver has the same type as the real declaration's,
        // i.e. some superclass of the current class. This is not good for accessibility checks.
        dispatchReceiverParameter?.type = irClass.defaultType
        annotations = fakeOverride.annotations
        copyCorrespondingPropertyFrom(fakeOverride)
    }
}
