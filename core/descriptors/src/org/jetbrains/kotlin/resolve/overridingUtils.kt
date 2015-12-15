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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.utils.DFS
import java.util.*

fun <TDescriptor : CallableDescriptor> TDescriptor.findTopMostOverriddenDescriptors(): List<TDescriptor> {
    return DFS.dfs(
            listOf(this),
            { current -> current.overriddenDescriptors },
            object : DFS.CollectingNodeHandler<CallableDescriptor, CallableDescriptor, ArrayList<TDescriptor>>(ArrayList<TDescriptor>()) {
                override fun afterChildren(current: CallableDescriptor) {
                    if (current.overriddenDescriptors.isEmpty()) {
                        @Suppress("UNCHECKED_CAST")
                        result.add(current as TDescriptor)
                    }
                }
            })
}


fun <TDescriptor : CallableDescriptor> TDescriptor.findOriginalTopMostOverriddenDescriptors(): Set<TDescriptor> {
    return findTopMostOverriddenDescriptors().mapTo(LinkedHashSet<TDescriptor>()) {
        @Suppress("UNCHECKED_CAST")
        (it.original as TDescriptor)
    }
}


private fun CallableDescriptor.isVar(): Boolean =
        this is PropertyDescriptor && isVar

private fun CallableDescriptor.isVal(): Boolean =
        this is PropertyDescriptor && !isVar

private fun isMostSpecificByReturnTypeAndKind(
        type: KotlinType,
        returnTypeOwner: CallableDescriptor,
        descriptors: Collection<CallableDescriptor>,
        typeChecker: KotlinTypeChecker
): Boolean =
        descriptors.all {
            otherDescriptor ->
            otherDescriptor == returnTypeOwner ||
            (!(returnTypeOwner.isVal() && otherDescriptor.isVar()) &&
             otherDescriptor.returnType?.let { typeChecker.isSubtypeOf(type, it) } ?: true)
        }


fun <TDescriptor : CallableDescriptor> getOverriddenWithMostSpecificReturnTypeOrNull(
        typeChecker: KotlinTypeChecker,
        descriptors: Collection<TDescriptor>
): TDescriptor? {
    val candidates = arrayListOf<TDescriptor>()

    // Need this to avoid recursion on lazy types.
    if (descriptors.isEmpty()) {
        return null
    } else if (descriptors.size == 1) {
        return descriptors.first()
    }

    for (descriptor in descriptors) {
        val returnType = descriptor.returnType ?: continue

        if (isMostSpecificByReturnTypeAndKind(returnType, descriptor, descriptors, typeChecker)) {
            candidates.add(descriptor)
        }
        else if (!TypeUtils.canHaveSubtypes(typeChecker, returnType)) {
            return null
        }
    }

    if (candidates.isEmpty()) {
        return null
    }

    var withNonErrorReturnType: TDescriptor? = null
    var withNonFlexibleReturnType: TDescriptor? = null
    for (candidate in candidates) {
        val returnType = candidate.returnType ?: continue
        withNonErrorReturnType = candidate
        if (!returnType.isFlexible()) {
            withNonFlexibleReturnType = candidate
        }
    }
    return withNonFlexibleReturnType ?: withNonErrorReturnType
}