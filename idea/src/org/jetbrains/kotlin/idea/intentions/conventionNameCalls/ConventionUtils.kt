/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions.conventionNameCalls

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.findOriginalTopMostOverriddenDescriptors

fun KtExpression.isAnyEquals(): Boolean {
    val resolvedCall = resolveToCall() ?: return false
    return (resolvedCall.resultingDescriptor as? FunctionDescriptor)?.isAnyEquals() == true
}

fun FunctionDescriptor.isAnyEquals(): Boolean {
    val overriddenDescriptors = findOriginalTopMostOverriddenDescriptors()
    return overriddenDescriptors.any { it.fqNameUnsafe.asString() == "kotlin.Any.equals" }
}