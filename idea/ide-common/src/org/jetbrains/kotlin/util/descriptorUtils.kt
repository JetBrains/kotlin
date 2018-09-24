/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.CONFLICT
import org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE

import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.KotlinTypeCheckerImpl
import org.jetbrains.kotlin.types.typeUtil.equalTypesOrNulls

fun descriptorsEqualWithSubstitution(
        descriptor1: DeclarationDescriptor?,
        descriptor2: DeclarationDescriptor?,
        checkOriginals: Boolean = true
): Boolean {
    if (descriptor1 == descriptor2) return true
    if (descriptor1 == null || descriptor2 == null) return false
    if (checkOriginals && descriptor1.original != descriptor2.original) return false
    if (descriptor1 !is CallableDescriptor) return true
    descriptor2 as CallableDescriptor

    val typeChecker = KotlinTypeCheckerImpl.withAxioms(object: KotlinTypeChecker.TypeConstructorEquality {
        override fun equals(a: TypeConstructor, b: TypeConstructor): Boolean {
            val typeParam1 = a.declarationDescriptor as? TypeParameterDescriptor
            val typeParam2 = b.declarationDescriptor as? TypeParameterDescriptor
            if (typeParam1 != null
                && typeParam2 != null
                && typeParam1.containingDeclaration == descriptor1
                && typeParam2.containingDeclaration == descriptor2) {
                return typeParam1.index == typeParam2.index
            }

            return a == b
        }
    })

    if (!typeChecker.equalTypesOrNulls(descriptor1.returnType, descriptor2.returnType)) return false

    val parameters1 = descriptor1.valueParameters
    val parameters2 = descriptor2.valueParameters
    if (parameters1.size != parameters2.size) return false
    for ((param1, param2) in parameters1.zip(parameters2)) {
        if (!typeChecker.equalTypes(param1.type, param2.type)) return false
    }
    return true
}

fun ClassDescriptor.findCallableMemberBySignature(
        signature: CallableMemberDescriptor,
        allowOverridabilityConflicts: Boolean = false
): CallableMemberDescriptor? {
    val descriptorKind = if (signature is FunctionDescriptor) DescriptorKindFilter.FUNCTIONS else DescriptorKindFilter.VARIABLES
    return defaultType.memberScope
            .getContributedDescriptors(descriptorKind)
            .filterIsInstance<CallableMemberDescriptor>()
            .firstOrNull {
                if (it.containingDeclaration != this) return@firstOrNull false
                val overridability = OverridingUtil.DEFAULT.isOverridableBy(it as CallableDescriptor, signature, null).result
                overridability == OVERRIDABLE || (allowOverridabilityConflicts && overridability == CONFLICT)
            }
}

fun TypeConstructor.supertypesWithAny(): Collection<KotlinType> {
    val supertypes = supertypes
    val noSuperClass = supertypes
            .map { it.constructor.declarationDescriptor as? ClassDescriptor }
            .all  { it == null || it.kind == ClassKind.INTERFACE }
    return if (noSuperClass) supertypes + builtIns.anyType else supertypes
}

val ClassifierDescriptorWithTypeParameters.constructors: Collection<ConstructorDescriptor>
    get() = when (this) {
        is TypeAliasDescriptor -> this.constructors
        is ClassDescriptor -> this.constructors
        else -> emptyList()
    }

val ClassifierDescriptorWithTypeParameters.kind: ClassKind?
    get() = when (this) {
        is TypeAliasDescriptor -> classDescriptor?.kind
        is ClassDescriptor -> kind
        else -> null
    }

val DeclarationDescriptor.isJavaDescriptor
    get() = this is JavaClassDescriptor || this is JavaCallableMemberDescriptor