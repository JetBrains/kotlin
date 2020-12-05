/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("SpecialBuiltinMembers")

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.getSpecialSignatureInfo
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.sameAsBuiltinMethodWithErasedValueParameters
import org.jetbrains.kotlin.load.java.ClassicBuiltinSpecialProperties.getBuiltinSpecialPropertyGetterName
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.kotlin.computeJvmSignature
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.firstOverridden
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure

object BuiltinMethodsWithSpecialGenericSignature : SpecialGenericSignatures() {
    private val CallableMemberDescriptor.hasErasedValueParametersInJava: Boolean
        get() = computeJvmSignature() in ERASED_VALUE_PARAMETERS_SIGNATURES

    @JvmStatic
    fun getOverriddenBuiltinFunctionWithErasedValueParametersInJava(
        functionDescriptor: FunctionDescriptor
    ): FunctionDescriptor? {
        if (!functionDescriptor.name.sameAsBuiltinMethodWithErasedValueParameters) return null
        return functionDescriptor.firstOverridden { it.hasErasedValueParametersInJava } as FunctionDescriptor?
    }

    @JvmStatic
    fun getDefaultValueForOverriddenBuiltinFunction(functionDescriptor: FunctionDescriptor): TypeSafeBarrierDescription? {
        if (functionDescriptor.name !in ERASED_VALUE_PARAMETERS_SHORT_NAMES) return null
        return functionDescriptor.firstOverridden {
            it.computeJvmSignature() in SIGNATURE_TO_DEFAULT_VALUES_MAP.keys
        }?.let { SIGNATURE_TO_DEFAULT_VALUES_MAP[it.computeJvmSignature()] }
    }

    val Name.sameAsBuiltinMethodWithErasedValueParameters: Boolean
        get() = this in ERASED_VALUE_PARAMETERS_SHORT_NAMES

    fun CallableMemberDescriptor.isBuiltinWithSpecialDescriptorInJvm(): Boolean {
        if (!KotlinBuiltIns.isBuiltIn(this)) return false
        return getSpecialSignatureInfo()?.isObjectReplacedWithTypeParameter ?: false || doesOverrideBuiltinWithDifferentJvmName()
    }

    @JvmStatic
    fun CallableMemberDescriptor.getSpecialSignatureInfo(): SpecialSignatureInfo? {
        if (name !in ERASED_VALUE_PARAMETERS_SHORT_NAMES) return null

        val builtinSignature = firstOverridden { it is FunctionDescriptor && it.hasErasedValueParametersInJava }?.computeJvmSignature()
            ?: return null

        return getSpecialSignatureInfo(builtinSignature)
    }
}

object BuiltinMethodsWithDifferentJvmName : SpecialGenericSignatures() {
    val Name.sameAsRenamedInJvmBuiltin: Boolean
        get() = this in ORIGINAL_SHORT_NAMES

    fun getJvmName(functionDescriptor: SimpleFunctionDescriptor): Name? {
        return SIGNATURE_TO_JVM_REPRESENTATION_NAME[functionDescriptor.computeJvmSignature() ?: return null]
    }

    fun isBuiltinFunctionWithDifferentNameInJvm(functionDescriptor: SimpleFunctionDescriptor): Boolean {
        return KotlinBuiltIns.isBuiltIn(functionDescriptor) && functionDescriptor.firstOverridden {
            SIGNATURE_TO_JVM_REPRESENTATION_NAME.containsKey(functionDescriptor.computeJvmSignature())
        } != null
    }

    fun getBuiltinFunctionNamesByJvmName(name: Name): List<Name> =
        JVM_SHORT_NAME_TO_BUILTIN_SHORT_NAMES_MAP[name] ?: emptyList()


    val SimpleFunctionDescriptor.isRemoveAtByIndex: Boolean
        get() = name.asString() == "removeAt" && computeJvmSignature() == REMOVE_AT_NAME_AND_SIGNATURE.signature
}

@Suppress("UNCHECKED_CAST")
fun <T : CallableMemberDescriptor> T.getOverriddenBuiltinWithDifferentJvmName(): T? {
    if (name !in SpecialGenericSignatures.ORIGINAL_SHORT_NAMES
        && propertyIfAccessor.name !in BuiltinSpecialProperties.SPECIAL_SHORT_NAMES
    ) return null

    return when (this) {
        is PropertyDescriptor, is PropertyAccessorDescriptor ->
            firstOverridden { ClassicBuiltinSpecialProperties.hasBuiltinSpecialPropertyFqName(it.propertyIfAccessor) } as T?
        is SimpleFunctionDescriptor ->
            firstOverridden {
                BuiltinMethodsWithDifferentJvmName.isBuiltinFunctionWithDifferentNameInJvm(it as SimpleFunctionDescriptor)
            } as T?
        else -> null
    }
}

fun CallableMemberDescriptor.doesOverrideBuiltinWithDifferentJvmName(): Boolean = getOverriddenBuiltinWithDifferentJvmName() != null

@Suppress("UNCHECKED_CAST")
fun <T : CallableMemberDescriptor> T.getOverriddenSpecialBuiltin(): T? {
    getOverriddenBuiltinWithDifferentJvmName()?.let { return it }

    if (!name.sameAsBuiltinMethodWithErasedValueParameters) return null

    return firstOverridden {
        KotlinBuiltIns.isBuiltIn(it) && it.getSpecialSignatureInfo() != null
    } as T?
}

// The subtle difference between getOverriddenBuiltinReflectingJvmDescriptor and getOverriddenSpecialBuiltin
// is that first one return descriptor reflecting JVM signature (JVM descriptor)
// E.g. it returns `contains(e: E): Boolean` instead of `contains(e: String): Boolean` for implementation of Collection<String>.contains
// Implementation differs by getting 'original' for collection methods with erased value parameters
// Also it ignores Collection<String>.containsAll overrides because they have the same JVM descriptor
@Suppress("UNCHECKED_CAST")
fun <T : CallableMemberDescriptor> T.getOverriddenBuiltinReflectingJvmDescriptor(): T? {
    getOverriddenBuiltinWithDifferentJvmName()?.let { return it }

    if (!name.sameAsBuiltinMethodWithErasedValueParameters) return null

    return firstOverridden {
        KotlinBuiltIns.isBuiltIn(it) && it.getSpecialSignatureInfo()?.isObjectReplacedWithTypeParameter ?: false
    }?.original as T?
}

fun getJvmMethodNameIfSpecial(callableMemberDescriptor: CallableMemberDescriptor): String? {
    val overriddenBuiltin = getOverriddenBuiltinThatAffectsJvmName(callableMemberDescriptor)?.propertyIfAccessor
        ?: return null
    return when (overriddenBuiltin) {
        is PropertyDescriptor -> overriddenBuiltin.getBuiltinSpecialPropertyGetterName()
        is SimpleFunctionDescriptor -> BuiltinMethodsWithDifferentJvmName.getJvmName(overriddenBuiltin)?.asString()
        else -> null
    }
}

private fun getOverriddenBuiltinThatAffectsJvmName(
    callableMemberDescriptor: CallableMemberDescriptor
): CallableMemberDescriptor? =
    if (KotlinBuiltIns.isBuiltIn(callableMemberDescriptor)) callableMemberDescriptor.getOverriddenBuiltinWithDifferentJvmName()
    else null

fun ClassDescriptor.hasRealKotlinSuperClassWithOverrideOf(
    specialCallableDescriptor: CallableDescriptor
): Boolean {
    val builtinContainerDefaultType = (specialCallableDescriptor.containingDeclaration as ClassDescriptor).defaultType

    var superClassDescriptor = DescriptorUtils.getSuperClassDescriptor(this)

    while (superClassDescriptor != null) {
        if (superClassDescriptor !is JavaClassDescriptor) {
            // Kotlin class

            val doesOverrideBuiltinDeclaration =
                TypeCheckingProcedure.findCorrespondingSupertype(superClassDescriptor.defaultType, builtinContainerDefaultType) != null

            if (doesOverrideBuiltinDeclaration) {
                return !KotlinBuiltIns.isBuiltIn(superClassDescriptor)
            }
        }

        superClassDescriptor = DescriptorUtils.getSuperClassDescriptor(superClassDescriptor)
    }

    return false
}

val CallableMemberDescriptor.isFromJava: Boolean
    get() {
        val descriptor = propertyIfAccessor
        return descriptor.containingDeclaration is JavaClassDescriptor
    }

fun CallableMemberDescriptor.isFromJavaOrBuiltins() = isFromJava || KotlinBuiltIns.isBuiltIn(this)
