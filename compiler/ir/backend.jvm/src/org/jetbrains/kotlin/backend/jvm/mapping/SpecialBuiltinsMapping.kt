/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.mapping

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.jvm.ir.isStaticInlineClassReplacement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.IrBasedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.getSpecialSignatureInfo
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.sameAsBuiltinMethodWithErasedValueParameters
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures
import org.jetbrains.kotlin.load.java.getOverriddenBuiltinWithDifferentJvmName
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.load.kotlin.signatures
import org.jetbrains.kotlin.resolve.descriptorUtil.firstOverridden
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature

class SpecialBuiltinsMapping(private val typeMapper: IrTypeMapper, private val signatureMapper: MethodSignatureMapper) {
    internal fun mapOverriddenSpecialBuiltinIfNeeded(callee: IrFunction, superCall: Boolean): JvmMethodSignature? {
        // Do not remap calls to static replacements of inline class methods, since they have completely different signatures.
        if (callee.isStaticInlineClassReplacement) return null
        val overriddenSpecialBuiltinFunction =
            (callee.toIrBasedDescriptor().getOverriddenBuiltinReflectingJvmDescriptor() as IrBasedSimpleFunctionDescriptor?)?.owner
        if (overriddenSpecialBuiltinFunction != null && !superCall) {
            return signatureMapper.mapSignatureSkipGeneric(overriddenSpecialBuiltinFunction)
        }

        return null
    }

    // The subtle difference between getOverriddenBuiltinReflectingJvmDescriptor and getOverriddenSpecialBuiltin
    // is that first one return descriptor reflecting JVM signature (JVM descriptor)
    // E.g. it returns `contains(e: E): Boolean` instead of `contains(e: String): Boolean` for implementation of Collection<String>.contains
    // Implementation differs by getting 'original' for collection methods with erased value parameters
    // Also it ignores Collection<String>.containsAll overrides because they have the same JVM descriptor
    @Suppress("UNCHECKED_CAST")
    @OptIn(K1Deprecation::class)
    fun <T : CallableMemberDescriptor> T.getOverriddenBuiltinReflectingJvmDescriptor(): T? {
        getOverriddenBuiltinWithDifferentJvmName()?.let { return it }

        if (!name.sameAsBuiltinMethodWithErasedValueParameters) return null

        return firstOverridden {
            KotlinBuiltIns.isBuiltIn(it) && it.getSpecialSignatureInfo()?.isObjectReplacedWithTypeParameter ?: false
        }?.original as T?
    }

    internal fun getDifferentNameForJvmBuiltinFunction(function: IrSimpleFunction): String? {
        if (function.name !in SpecialGenericSignatures.ORIGINAL_SHORT_NAMES) return null
        if (!function.isBuiltIn) return null
        return function.allOverridden(includeSelf = true)
            .filter { it.isBuiltIn }
            .firstNotNullOfOrNull {
                val signature = function.computeJvmSignature()
                SpecialGenericSignatures.SIGNATURE_TO_JVM_REPRESENTATION_NAME[signature]?.asString()
            }
    }

    internal fun getBuiltinSpecialPropertyGetterName(function: IrSimpleFunction): String? {
        val propertyName = function.correspondingPropertySymbol?.owner?.name ?: return null
        if (propertyName !in BuiltinSpecialProperties.SPECIAL_SHORT_NAMES) return null
        if (!function.isBuiltIn) return null
        return function.allOverridden(includeSelf = true).firstNotNullOfOrNull {
            val property = it.correspondingPropertySymbol!!.owner
            BuiltinSpecialProperties.PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP[property.fqNameWhenAvailable]?.asString()
        }
    }

    private val IrSimpleFunction.isBuiltIn: Boolean
        get() = getPackageFragment().packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME ||
                (parent as? IrClass)?.fqNameWhenAvailable?.toUnsafe()?.let(JavaToKotlinClassMap::mapKotlinToJava) != null

    internal fun IrFunction.computeJvmSignature(): String = signatures {
        val classPart = typeMapper.mapType(parentAsClass.defaultType).internalName
        val signature = signatureMapper.mapSignature(this@computeJvmSignature, skipGenericSignature = false, skipSpecial = true).toString()
        return SignatureBuildingComponents.signature(classPart, signature)
    }
}
