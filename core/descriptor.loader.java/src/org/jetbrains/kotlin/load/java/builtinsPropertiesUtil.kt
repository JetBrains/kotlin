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

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.check

private val BUILTIN_SPECIAL_PROPERTIES_FQ_NAMES = setOf(FqName("kotlin.Collection.size"), FqName("kotlin.Map.size"))
private val BUILTIN_SPECIAL_PROPERTIES_SHORT_NAMES = BUILTIN_SPECIAL_PROPERTIES_FQ_NAMES.map { it.shortName() }.toSet()


private val BUILTIN_METHODS_ERASED_COLLECTION_PARAMETER_FQ_NAMES = setOf(FqName("kotlin.Collection.containsAll"))
private val BUILTIN_METHODS_GENERIC_PARAMETERS_FQ_NAMES = setOf(FqName("kotlin.Collection.contains"))

private val BUILTIN_METHODS_ERASED_VALUE_PARAMETERS_FQ_NAMES =
        BUILTIN_METHODS_GENERIC_PARAMETERS_FQ_NAMES + BUILTIN_METHODS_ERASED_COLLECTION_PARAMETER_FQ_NAMES

private val BUILTIN_METHODS_ERASED_VALUE_PARAMETERS_SHORT_NAMES =
        BUILTIN_METHODS_ERASED_VALUE_PARAMETERS_FQ_NAMES.map { it.shortName() }.toSet()

public fun CallableDescriptor.hasBuiltinSpecialPropertyFqName(): Boolean {
    if (this is PropertyAccessorDescriptor) return correspondingProperty.hasBuiltinSpecialPropertyFqName()
    if (name !in BUILTIN_SPECIAL_PROPERTIES_SHORT_NAMES) return false

    return hasBuiltinSpecialPropertyFqNameImpl()
}

private fun CallableDescriptor.hasBuiltinSpecialPropertyFqNameImpl(): Boolean {
    if (fqNameOrNull() in BUILTIN_SPECIAL_PROPERTIES_FQ_NAMES) return true

    if (!fqNameUnsafe.firstSegmentIs(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME)) return false
    if (builtIns.builtInsModule != module) return false

    return overriddenDescriptors.any(CallableDescriptor::hasBuiltinSpecialPropertyFqName)
}

private fun CallableDescriptor.fqNameOrNull(): FqName? = fqNameUnsafe.check { it.isSafe }?.toSafe()

val Name.isBuiltinSpecialPropertyName: Boolean get() = this in BUILTIN_SPECIAL_PROPERTIES_SHORT_NAMES

public fun CallableMemberDescriptor.getBuiltinSpecialPropertyAccessorName(): String? {
    return propertyIfAccessor.check { it.hasBuiltinSpecialPropertyFqName() }?.name?.asString()
}

@Suppress("UNCHECKED_CAST")
fun <T : CallableMemberDescriptor> T.getBuiltinSpecialOverridden(): T? {
    return firstOverridden { it.propertyIfAccessor.hasBuiltinSpecialPropertyFqName() } as T?
}

fun CallableMemberDescriptor.overridesBuiltinSpecialDeclaration(): Boolean = getBuiltinSpecialOverridden() != null

public fun CallableMemberDescriptor.getJvmMethodNameIfSpecial(): String? {
    return getBuiltinOverriddenThatAffectsJvmName()?.getBuiltinSpecialPropertyAccessorName()
}

private fun CallableMemberDescriptor.getBuiltinOverriddenThatAffectsJvmName(): CallableMemberDescriptor? {
    return if (hasBuiltinSpecialPropertyFqName() || original.isFromJava) getBuiltinSpecialOverridden() else null
}

private val CallableMemberDescriptor.isFromJava: Boolean
    get() = propertyIfAccessor is JavaCallableMemberDescriptor

private val CallableMemberDescriptor.propertyIfAccessor: CallableDescriptor
    get() = if (this is PropertyAccessorDescriptor) correspondingProperty else this

val CallableMemberDescriptor.hasErasedValueParametersInJava: Boolean
    get() = fqNameOrNull() in BUILTIN_METHODS_ERASED_VALUE_PARAMETERS_FQ_NAMES

fun FunctionDescriptor.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(): FunctionDescriptor? {
    if (!name.sameAsBuiltinMethodWithErasedValueParameters) return null
    return firstOverridden { it.hasErasedValueParametersInJava } as FunctionDescriptor?
}

private fun CallableMemberDescriptor.firstOverridden(
        predicate: (CallableMemberDescriptor) -> Boolean
): CallableMemberDescriptor? {
    var result: CallableMemberDescriptor? = null
    return DFS.dfs(listOf(this),
        object : DFS.Neighbors<CallableMemberDescriptor> {
            override fun getNeighbors(current: CallableMemberDescriptor?): Iterable<CallableMemberDescriptor> {
                return current?.overriddenDescriptors ?: emptyList()
            }
        },
        object : DFS.AbstractNodeHandler<CallableMemberDescriptor, CallableMemberDescriptor?>() {
            override fun beforeChildren(current: CallableMemberDescriptor) = result == null
            override fun afterChildren(current: CallableMemberDescriptor) {
                if (result == null && predicate(current)) {
                    result = current
                }
            }
            override fun result(): CallableMemberDescriptor? = result
        }
    )
}

val Name.sameAsBuiltinMethodWithErasedValueParameters: Boolean
    get () = this in BUILTIN_METHODS_ERASED_VALUE_PARAMETERS_SHORT_NAMES

enum class SpecialSignatureInfo(val signature: String?) {
    ONE_COLLECTION_PARAMETER("(Ljava/util/Collection<+Ljava/lang/Object;>;)Z"),
    GENERIC_PARAMETER(null)
}

fun CallableMemberDescriptor.getSpecialSignatureInfo(): SpecialSignatureInfo? {
    val builtinFqName = firstOverridden { it is FunctionDescriptor && it.hasErasedValueParametersInJava }?.fqNameOrNull()
            ?: return null

    return when (builtinFqName) {
        in BUILTIN_METHODS_ERASED_COLLECTION_PARAMETER_FQ_NAMES -> SpecialSignatureInfo.ONE_COLLECTION_PARAMETER
        in BUILTIN_METHODS_GENERIC_PARAMETERS_FQ_NAMES -> SpecialSignatureInfo.GENERIC_PARAMETER
        else -> error("Unexpected kind of special builtin: $builtinFqName")
    }
}
