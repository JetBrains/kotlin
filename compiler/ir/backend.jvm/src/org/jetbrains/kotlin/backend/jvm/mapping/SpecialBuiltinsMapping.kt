/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.mapping

import org.jetbrains.kotlin.backend.jvm.ir.isStaticInlineClassReplacement
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.Companion.ERASED_VALUE_PARAMETERS_SHORT_NAMES
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.Companion.ERASED_VALUE_PARAMETERS_SIGNATURES
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.SpecialSignatureInfo
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.load.kotlin.signatures
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature

class SpecialBuiltinsMapping(private val typeMapper: IrTypeMapper, private val signatureMapper: MethodSignatureMapper) {
    internal fun mapOverriddenSpecialBuiltinIfNeeded(callee: IrFunction, superCall: Boolean): JvmMethodSignature? {
        // Do not remap calls to static replacements of inline class methods, since they have completely different signatures.
        if (callee.isStaticInlineClassReplacement) return null

        if (callee !is IrSimpleFunction) return null
        val overriddenSpecialBuiltinFunction = callee.getOverriddenBuiltinReflectingJvmDescriptor()
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
    fun IrSimpleFunction.getOverriddenBuiltinReflectingJvmDescriptor(): IrSimpleFunction? {
        getOverriddenBuiltinWithDifferentJvmName()?.let { return it }

        if (!name.sameAsBuiltinMethodWithErasedValueParameters) return null

        return firstOverridden {
            it.isBuiltIn && it.getSpecialSignatureInfo()?.isObjectReplacedWithTypeParameter == true
        }
    }

    fun IrSimpleFunction.getOverriddenBuiltinWithDifferentJvmName(): IrSimpleFunction? {
        if (name !in SpecialGenericSignatures.ORIGINAL_SHORT_NAMES &&
            propertyIfAccessor.name !in BuiltinSpecialProperties.SPECIAL_SHORT_NAMES
        ) return null

        return when {
            isPropertyAccessor -> firstOverridden {
                hasBuiltinSpecialPropertyFqName(it.correspondingPropertySymbol!!.owner)
            }
            else -> firstOverridden {
                getDifferentNameForJvmBuiltinFunction(it) != null
            }
        }
    }

    private fun IrSimpleFunction.getSpecialSignatureInfo(): SpecialSignatureInfo? {
        if (name !in ERASED_VALUE_PARAMETERS_SHORT_NAMES) return null

        val builtinSignature = firstOverridden { it.hasErasedValueParametersInJava }?.computeJvmSignature()
            ?: return null

        return SpecialGenericSignatures.getSpecialSignatureInfo(builtinSignature)
    }

    private val IrSimpleFunction.hasErasedValueParametersInJava: Boolean
        get() = computeJvmSignature() in ERASED_VALUE_PARAMETERS_SIGNATURES

    private val Name.sameAsBuiltinMethodWithErasedValueParameters: Boolean
        get() = this in ERASED_VALUE_PARAMETERS_SHORT_NAMES

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

    private fun hasBuiltinSpecialPropertyFqName(property: IrProperty): Boolean =
        property.name in BuiltinSpecialProperties.SPECIAL_SHORT_NAMES && property.hasBuiltinSpecialPropertyFqNameImpl()

    private fun IrProperty.hasBuiltinSpecialPropertyFqNameImpl(): Boolean {
        if (fqNameWhenAvailable in BuiltinSpecialProperties.SPECIAL_FQ_NAMES) return true
        return isBuiltIn && overriddenSymbols.any { hasBuiltinSpecialPropertyFqName(it.owner) }
    }

    private val IrDeclaration.isBuiltIn: Boolean
        get() = getPackageFragment().packageFqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME ||
                (parent as? IrClass)?.fqNameWhenAvailable?.toUnsafe()?.let(JavaToKotlinClassMap::mapKotlinToJava) != null

    internal fun IrFunction.computeJvmSignature(): String = signatures {
        val classPart = typeMapper.mapType(parentAsClass.defaultType).internalName
        val signature = signatureMapper.mapSignature(this@computeJvmSignature, skipGenericSignature = false, skipSpecial = true).toString()
        return SignatureBuildingComponents.signature(classPart, signature)
    }

    private inline fun <T : IrOverridableDeclaration<*>> T.firstOverridden(predicate: (T) -> Boolean): T? =
        allOverridden(includeSelf = true).firstOrNull(predicate)
}
