/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jklib

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrMangleComputer
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.jvm.serialization.BaseJvmIrMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.typeEnhancement.hasEnhancedNullability
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.TypeArgumentMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.kotlin.types.model.TypeSystemContext
import org.jetbrains.kotlin.types.model.TypeVariance
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations
import org.jetbrains.kotlin.utils.DFS.ifAny as ifAnyDFS

/*
 * The manglers defined in this file compute plain JVM signatures for Java methods.
 * For example instead of this:
 *  ```
 *  java/lang/Comparator.thenComparing(java.util.function.Function<in|1:0?,out|0:0?>?){0§<kotlin.Comparable<in|0:0?>?>}
 *  ```
 *  We will compute:
 *  ```
 *  Ljava/util/Comparator.thenComparing(Ljava/util/function/Function;)Ljava/util/Comparator;
 *  ```
 * This logic aims to fix signature differences between K1 and K2.
 * TODO(KT-81659): When the klib deserialization part transitions to K2, code in this file should no longer be needed and we could
 * directly reuse manglers from Kotlin/JVM.
 */

class JKlibIrMangler : BaseJvmIrMangler() {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun IrDeclaration.signatureString(compatibleMode: Boolean): String {
        if (!getPackageFragment().packageFqName.asString().isKotlinPackage() && isJavaBackedCallable()) {
            (descriptor as? CallableDescriptor)?.computeJvmSignatureSafe()?.let {
                return it
            }
        }
        return getMangleComputer(MangleMode.SIGNATURE, compatibleMode).computeMangle(this)
    }

    private class JKlibIrManglerComputer(builder: StringBuilder, mode: MangleMode, compatibleMode: Boolean) :
        JvmIrManglerComputer(builder, mode, compatibleMode, useEffectiveTypeVariances = true) {
        override fun copy(newMode: MangleMode): IrMangleComputer =
            JKlibIrManglerComputer(builder, newMode, compatibleMode)

        override fun addReturnTypeSpecialCase(function: IrFunction): Boolean = false

        override fun mangleTypePlatformSpecific(type: IrType, tBuilder: StringBuilder) {
            if (type.hasAnnotation(JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION)) {
                tBuilder.append(MangleConstant.ENHANCED_NULLABILITY_MARK)
            }
        }
    }

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<IrDeclaration> =
        JKlibIrManglerComputer(StringBuilder(256), mode, compatibleMode)
}

class JKlibDescriptorMangler(private val mainDetector: MainFunctionDetector?) : JvmDescriptorMangler(mainDetector) {

    override fun DeclarationDescriptor.signatureString(compatibleMode: Boolean): String {
        if (this.containingPackage()?.asString()
                ?.isKotlinPackage() == false && this is JavaCallableMemberDescriptor || containingDeclaration is JavaClassDescriptor
        ) {
            (this as? CallableDescriptor)?.computeJvmSignatureSafe()?.let {
                return it
            }
        }
        return getMangleComputer(MangleMode.SIGNATURE, compatibleMode).computeMangle(this)
    }

    private class JKlibDescriptorManglerComputer(
        builder: StringBuilder,
        private val mainDetector: MainFunctionDetector?,
        mode: MangleMode,
    ) : JvmDescriptorManglerComputer(builder, mainDetector, mode, useEffectiveTypeVariances = true) {
        override fun addReturnTypeSpecialCase(function: FunctionDescriptor): Boolean = false

        override fun copy(newMode: MangleMode): DescriptorMangleComputer = JKlibDescriptorManglerComputer(builder, mainDetector, newMode)
    }

    override fun getMangleComputer(mode: MangleMode, compatibleMode: Boolean): KotlinMangleComputer<DeclarationDescriptor> =
        JKlibDescriptorManglerComputer(StringBuilder(256), mainDetector, mode)
}

private fun String.isKotlinPackage(): Boolean {
    return this == "kotlin" || startsWith("kotlin.")
}

private fun StringBuilder.appendErasedType(type: KotlinType) {
    append(type.mapToJvmType())
}

private fun KotlinType.mapToJvmType(): JvmType {
    var type = this
    // Under JSpecify strict mode, Kotlinc loads non-nullable enhanced Java boxed types (like `@NonNull Integer` or `@NullMarked` Java
    // `Boolean`) as non-nullable Kotlin primitive types (`kotlin.Int`, `kotlin.Boolean`) with a `@EnhancedNullability` annotation.
    //
    // However, K2's IR-backed descriptors used during Klib serialization lose this `@EnhancedNullability` annotation. Consequently, the
    // `mapType()` call below maps them to primitive JVM types (`I`, `Z`) in Klib metadata signatures instead of boxed types.
    //
    // The Klib deserialization process using K1 does not have this issue and correctly maps the type leading to some signature mismatches
    // during the IR linking.
    //
    // To resolve this discrepancy, we strip the `hasEnhancedNullability` annotations in J2CL's mangler. This matches K2's "stripped"
    // behavior, forcing `mapType` to map them to primitive descriptors (`I`, `Z`) to ensure they link successfully. True Java primitives
    // (which never had `@EnhancedNullability`) are unaffected.
    // TODO(KT-86165): A proper long-term solution would be to avoid using IrBasedDescriptors at all in K2 and compute the JVM signature
    //  from the IR.
    if (
        type is SimpleType &&
        KotlinBuiltIns.isPrimitiveType(type) &&
        !type.isNullable() &&
        type.hasEnhancedNullability()
    ) {
        type = type.replaceAnnotations(Annotations.EMPTY)
    }

    return mapType(
        type,
        JvmTypeFactoryImpl,
        TypeMappingMode.DEFAULT,
        TypeMappingConfigurationImpl,
        descriptorTypeWriter = null,
    )
}

private fun hasVoidReturnType(descriptor: CallableDescriptor): Boolean {
    if (descriptor is ConstructorDescriptor) return true
    return KotlinBuiltIns.isUnit(descriptor.returnType!!) && !TypeUtils.isNullableType(descriptor.returnType!!)
            && descriptor !is PropertyGetterDescriptor
}

private fun FunctionDescriptor.computeJvmDescriptor(withReturnType: Boolean = true, withName: Boolean = true): String = buildString {
    if (withName) {
        append(if (this@computeJvmDescriptor is ConstructorDescriptor) "<init>" else name.asString())
    }

    append("(")

    extensionReceiverParameter?.let {
        appendErasedType(it.type)
    }

    for (parameter in valueParameters) {
        appendErasedType(parameter.type)
    }

    append(")")

    if (withReturnType) {
        if (hasVoidReturnType(this@computeJvmDescriptor)) {
            append("V")
        } else {
            appendErasedType(returnType!!)
        }
    }
}

private fun CallableDescriptor.computeJvmSignature(): String? = signatures {
    if (DescriptorUtils.isLocal(this@computeJvmSignature)) return null

    val classDescriptor = containingDeclaration as? ClassDescriptor ?: return null
    if (classDescriptor.name.isSpecial) return null

    signature(
        classDescriptor,
        (original as? FunctionDescriptor ?: return null).computeJvmDescriptor()
    )
}

// TODO(KT-84880): Replace with IR-based signature computation (MethodSignatureMapper?) 
private fun CallableDescriptor.computeJvmSignatureSafe(): String? {
    return try {
        computeJvmSignature()
    } catch (e: Exception) {
        Logger.getInstance(JKlibIrMangler::class.java).error("Failed to compute JVM signature for $this", e)
        null
    }
}

private fun IrDeclaration.isDeclaredInJava(): Boolean {
    if (origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) return true
    val ownerClass = parentClassOrNull
    return ownerClass?.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
}

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrDeclaration.isJavaBackedCallable(): Boolean {
    if (isDeclaredInJava()) return true

    val functions = when (this) {
        is IrSimpleFunction -> {
            // Check if the function a fake override of a Java declaration.
            listOf(this)
        }
        is IrProperty -> {
            // Check property accessors.
            listOfNotNull(getter, setter)
        }
        else -> emptyList()
    }

    if (functions.any { it.isFakeOverride }) {
        return ifAnyDFS(
            functions,
            { current ->
                if (current.isFakeOverride) current.overriddenSymbols.map { it.owner } else emptyList()
            },
            { current -> current.isDeclaredInJava() },
        )
    }
    return false
}
