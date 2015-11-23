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
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties.getBuiltinSpecialPropertyGetterName
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure

object BuiltinSpecialProperties {
    private val PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP = mapOf(
            FqName("kotlin.Enum.name")                 to Name.identifier("name"),
            FqName("kotlin.Enum.ordinal")              to Name.identifier("ordinal"),
            FqName("kotlin.Collection.size")           to Name.identifier("size"),
            FqName("kotlin.Map.size")                  to Name.identifier("size"),
            FqName("kotlin.CharSequence.length")       to Name.identifier("length"),
            FqName("kotlin.Map.keys")                  to Name.identifier("keySet"),
            FqName("kotlin.Map.values")                to Name.identifier("values"),
            FqName("kotlin.Map.entries")               to Name.identifier("entrySet")
    )

    private val GETTER_JVM_NAME_TO_PROPERTIES_SHORT_NAME_MAP: Map<Name, List<Name>> =
            PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP.getInversedShortNamesMap()

    private val FQ_NAMES = PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP.keys
    internal val SHORT_NAMES = FQ_NAMES.map { it.shortName() }.toSet()

    fun hasBuiltinSpecialPropertyFqName(callableMemberDescriptor: CallableMemberDescriptor): Boolean {
        if (callableMemberDescriptor.name !in SHORT_NAMES) return false

        return callableMemberDescriptor.hasBuiltinSpecialPropertyFqNameImpl()
    }

    private fun CallableMemberDescriptor.hasBuiltinSpecialPropertyFqNameImpl(): Boolean {
        if (fqNameOrNull() in FQ_NAMES) return true
        if (!isFromBuiltins()) return false

        return overriddenDescriptors.any { hasBuiltinSpecialPropertyFqName(it) }
    }

    fun getPropertyNameCandidatesBySpecialGetterName(name1: Name): List<Name> =
            GETTER_JVM_NAME_TO_PROPERTIES_SHORT_NAME_MAP[name1] ?: emptyList()

    fun CallableMemberDescriptor.getBuiltinSpecialPropertyGetterName(): String? {
        assert(isFromBuiltins()) { "This method is defined only for builtin members, but $this found" }

        val descriptor = propertyIfAccessor.firstOverridden { hasBuiltinSpecialPropertyFqName(it) } ?: return null
        return PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP[descriptor.fqNameSafe]?.asString()
    }
}

object BuiltinMethodsWithSpecialGenericSignature {
    private val ERASED_COLLECTION_PARAMETER_FQ_NAMES = setOf(
            FqName("kotlin.Collection.containsAll"),
            FqName("kotlin.MutableCollection.removeAll"),
            FqName("kotlin.MutableCollection.retainAll")
    )

    enum class DefaultValue(val value: Any?) {
        NULL(null), INDEX(-1), FALSE(false)
    }

    private val GENERIC_PARAMETERS_METHODS_TO_DEFAULT_VALUES_MAP = mapOf(
            FqName("kotlin.Collection.contains")      to DefaultValue.FALSE,
            FqName("kotlin.MutableCollection.remove") to DefaultValue.FALSE,
            FqName("kotlin.Map.containsKey")          to DefaultValue.FALSE,
            FqName("kotlin.Map.containsValue")        to DefaultValue.FALSE,

            FqName("kotlin.Map.get")                  to DefaultValue.NULL,
            FqName("kotlin.MutableMap.remove")        to DefaultValue.NULL,

            FqName("kotlin.List.indexOf")             to DefaultValue.INDEX,
            FqName("kotlin.List.lastIndexOf")         to DefaultValue.INDEX
    )

    private val ERASED_VALUE_PARAMETERS_FQ_NAMES =
            GENERIC_PARAMETERS_METHODS_TO_DEFAULT_VALUES_MAP.keys + ERASED_COLLECTION_PARAMETER_FQ_NAMES

    private val ERASED_VALUE_PARAMETERS_SHORT_NAMES =
            ERASED_VALUE_PARAMETERS_FQ_NAMES.map { it.shortName() }.toSet()

    private val CallableMemberDescriptor.hasErasedValueParametersInJava: Boolean
        get() = fqNameOrNull() in ERASED_VALUE_PARAMETERS_FQ_NAMES

    @JvmStatic
    fun getOverriddenBuiltinFunctionWithErasedValueParametersInJava(
            functionDescriptor: FunctionDescriptor
    ): FunctionDescriptor? {
        if (!functionDescriptor.name.sameAsBuiltinMethodWithErasedValueParameters) return null
        return functionDescriptor.firstOverridden { it.hasErasedValueParametersInJava } as FunctionDescriptor?
    }

    @JvmStatic
    fun getDefaultValueForOverriddenBuiltinFunction(functionDescriptor: FunctionDescriptor): DefaultValue? {
        if (functionDescriptor.name !in ERASED_VALUE_PARAMETERS_SHORT_NAMES) return null
        return functionDescriptor.firstOverridden {
            it.fqNameOrNull() in GENERIC_PARAMETERS_METHODS_TO_DEFAULT_VALUES_MAP.keys
        }?.let { GENERIC_PARAMETERS_METHODS_TO_DEFAULT_VALUES_MAP[it.fqNameSafe] }
    }

    val Name.sameAsBuiltinMethodWithErasedValueParameters: Boolean
        get () = this in ERASED_VALUE_PARAMETERS_SHORT_NAMES

    enum class SpecialSignatureInfo(val valueParametersSignature: String?, val isObjectReplacedWithTypeParameter: Boolean) {
        ONE_COLLECTION_PARAMETER("Ljava/util/Collection<+Ljava/lang/Object;>;", false),
        OBJECT_PARAMETER_NON_GENERIC(null, true),
        OBJECT_PARAMETER_GENERIC("Ljava/lang/Object;", true)
    }

    fun CallableMemberDescriptor.isBuiltinWithSpecialDescriptorInJvm(): Boolean {
        if (!isFromBuiltins()) return false
        return getSpecialSignatureInfo()?.isObjectReplacedWithTypeParameter ?: false || doesOverrideBuiltinWithDifferentJvmName()
    }

    @JvmStatic
    fun CallableMemberDescriptor.getSpecialSignatureInfo(): SpecialSignatureInfo? {
        val builtinFqName = firstOverridden { it is FunctionDescriptor && it.hasErasedValueParametersInJava }?.fqNameOrNull()
                ?: return null

        if (builtinFqName in ERASED_COLLECTION_PARAMETER_FQ_NAMES) return SpecialSignatureInfo.ONE_COLLECTION_PARAMETER

        val defaultValue = GENERIC_PARAMETERS_METHODS_TO_DEFAULT_VALUES_MAP[builtinFqName]!!

        return if (defaultValue == DefaultValue.NULL)
                    // return type is some generic type as 'Map.get'
                    SpecialSignatureInfo.OBJECT_PARAMETER_GENERIC
                else
                    SpecialSignatureInfo.OBJECT_PARAMETER_NON_GENERIC
    }
}

object BuiltinMethodsWithDifferentJvmName {
    val REMOVE_AT_FQ_NAME = FqName("kotlin.MutableList.removeAt")

    val FQ_NAMES_TO_JVM_MAP: Map<FqName, Name> = mapOf(
            FqName("kotlin.Number.toByte")    to Name.identifier("byteValue"),
            FqName("kotlin.Number.toShort")   to Name.identifier("shortValue"),
            FqName("kotlin.Number.toInt")     to Name.identifier("intValue"),
            FqName("kotlin.Number.toLong")    to Name.identifier("longValue"),
            FqName("kotlin.Number.toFloat")   to Name.identifier("floatValue"),
            FqName("kotlin.Number.toDouble")  to Name.identifier("doubleValue"),
            REMOVE_AT_FQ_NAME                 to Name.identifier("remove"),
            FqName("kotlin.CharSequence.get") to Name.identifier("charAt")
    )

    val ORIGINAL_SHORT_NAMES: List<Name> = FQ_NAMES_TO_JVM_MAP.keys.map { it.shortName() }

    val JVM_SHORT_NAME_TO_BUILTIN_SHORT_NAMES_MAP: Map<Name, List<Name>> = FQ_NAMES_TO_JVM_MAP.getInversedShortNamesMap()

    val Name.sameAsRenamedInJvmBuiltin: Boolean
        get() = this in ORIGINAL_SHORT_NAMES

    fun getJvmName(callableMemberDescriptor: CallableMemberDescriptor): Name? {
        return FQ_NAMES_TO_JVM_MAP[callableMemberDescriptor.fqNameOrNull() ?: return null]
    }

    fun isBuiltinFunctionWithDifferentNameInJvm(callableMemberDescriptor: CallableMemberDescriptor): Boolean {
        if (!callableMemberDescriptor.isFromBuiltins()) return false
        val fqName = callableMemberDescriptor.fqNameOrNull() ?: return false
        return callableMemberDescriptor.firstOverridden { FQ_NAMES_TO_JVM_MAP.containsKey(fqName) } != null
    }

    fun getBuiltinFunctionNamesByJvmName(name: Name): List<Name> =
            JVM_SHORT_NAME_TO_BUILTIN_SHORT_NAMES_MAP[name] ?: emptyList()


    val CallableMemberDescriptor.isRemoveAtByIndex: Boolean
        get() = name.asString() == "removeAt" && fqNameOrNull() == REMOVE_AT_FQ_NAME
}

@Suppress("UNCHECKED_CAST")
fun <T : CallableMemberDescriptor> T.getOverriddenBuiltinWithDifferentJvmName(): T? {
    if (name !in BuiltinMethodsWithDifferentJvmName.ORIGINAL_SHORT_NAMES
            && propertyIfAccessor.name !in BuiltinSpecialProperties.SHORT_NAMES) return null

    return when (this) {
        is PropertyDescriptor, is PropertyAccessorDescriptor ->
            firstOverridden { BuiltinSpecialProperties.hasBuiltinSpecialPropertyFqName(it.propertyIfAccessor) } as T?
        else -> firstOverridden { BuiltinMethodsWithDifferentJvmName.isBuiltinFunctionWithDifferentNameInJvm(it) } as T?
    }
}

fun CallableMemberDescriptor.doesOverrideBuiltinWithDifferentJvmName(): Boolean = getOverriddenBuiltinWithDifferentJvmName() != null

@Suppress("UNCHECKED_CAST")
fun <T : CallableMemberDescriptor> T.getOverriddenSpecialBuiltin(): T? {
    getOverriddenBuiltinWithDifferentJvmName()?.let { return it }

    if (!name.sameAsBuiltinMethodWithErasedValueParameters) return null

    return firstOverridden {
        it.isFromBuiltins() && it.getSpecialSignatureInfo() != null
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
        it.isFromBuiltins() && it.getSpecialSignatureInfo()?.isObjectReplacedWithTypeParameter ?: false
    }?.original as T?
}

fun getJvmMethodNameIfSpecial(callableMemberDescriptor: CallableMemberDescriptor): String? {
    if (callableMemberDescriptor.propertyIfAccessor.name == DescriptorUtils.ENUM_VALUES) {
        val containingDeclaration = callableMemberDescriptor.containingDeclaration
        if (callableMemberDescriptor is PropertyAccessorDescriptor
                && containingDeclaration is ClassDescriptor
                && containingDeclaration.kind == ClassKind.ENUM_CLASS) return DescriptorUtils.ENUM_VALUES.asString()
    }

    val overriddenBuiltin = getOverriddenBuiltinThatAffectsJvmName(callableMemberDescriptor)?.propertyIfAccessor
                            ?: return null
    return when (overriddenBuiltin) {
        is PropertyDescriptor -> overriddenBuiltin.getBuiltinSpecialPropertyGetterName()
        else -> BuiltinMethodsWithDifferentJvmName.getJvmName(overriddenBuiltin)?.asString()
    }
}

private fun getOverriddenBuiltinThatAffectsJvmName(
        callableMemberDescriptor: CallableMemberDescriptor
): CallableMemberDescriptor? {
    val overriddenBuiltin = callableMemberDescriptor.getOverriddenBuiltinWithDifferentJvmName() ?: return null

    if (callableMemberDescriptor.isFromBuiltins()) return overriddenBuiltin

    return null
}

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
                val containingPackageFragment = DescriptorUtils.getParentOfType(superClassDescriptor, PackageFragmentDescriptor::class.java)
                if (superClassDescriptor.builtIns.isBuiltInPackageFragment(containingPackageFragment)) return false
                return true
            }
        }

        superClassDescriptor = DescriptorUtils.getSuperClassDescriptor(superClassDescriptor)
    }

    return false
}

// Util methods
val CallableMemberDescriptor.isFromJava: Boolean
    get() = propertyIfAccessor is JavaCallableMemberDescriptor && propertyIfAccessor.containingDeclaration is JavaClassDescriptor

fun CallableMemberDescriptor.isFromBuiltins(): Boolean {
    val fqName = propertyIfAccessor.fqNameOrNull() ?: return false
    return fqName.toUnsafe().startsWith(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME) &&
            this.module == this.builtIns.builtInsModule
}

fun CallableMemberDescriptor.isFromJavaOrBuiltins() = isFromJava || isFromBuiltins()

private fun Map<FqName, Name>.getInversedShortNamesMap(): Map<Name, List<Name>> =
        entries.groupBy { it.value }.mapValues { entry -> entry.value.map { it.key.shortName() } }