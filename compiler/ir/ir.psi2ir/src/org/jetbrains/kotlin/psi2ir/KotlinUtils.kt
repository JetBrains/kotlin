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

package org.jetbrains.kotlin.psi2ir

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi2ir.generators.getResolvedCall
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.upperIfFlexible

fun KotlinType.containsNull() =
        KotlinTypeChecker.DEFAULT.isSubtypeOf(builtIns.nullableNothingType, this.upperIfFlexible())

fun KtElement.deparenthesize(): KtElement =
        if (this is KtExpression) KtPsiUtil.safeDeparenthesize(this) else this

val CallableDescriptor.explicitReceiverType: KotlinType?
    get() {
        extensionReceiverParameter?.let { return it.type }
        dispatchReceiverParameter?.let { return it.type }
        return null
    }

fun ResolvedCall<*>.isValueArgumentReorderingRequired(): Boolean {
    var lastValueParameterIndex = -1
    for (valueArgument in call.valueArguments) {
        val argumentMapping = getArgumentMapping(valueArgument)
        if (argumentMapping !is ArgumentMatch || argumentMapping.isError()) {
            throw AssertionError("Value argument in function call is mapped with error")
        }
        val argumentIndex = argumentMapping.valueParameter.index
        if (argumentIndex < lastValueParameterIndex) {
            return true
        }
        lastValueParameterIndex = argumentIndex
    }
    return false
}

fun KtSecondaryConstructor.isConstructorDelegatingToSuper(bindingContext: BindingContext): Boolean {
    val delegatingResolvedCall = getDelegationCall().getResolvedCall(bindingContext) ?: return false
    val constructorDescriptor = bindingContext.get(BindingContext.CONSTRUCTOR, this) ?: return false
    val ownerClassDescriptor = constructorDescriptor.containingDeclaration
    val targetClassDescriptor = delegatingResolvedCall.resultingDescriptor.containingDeclaration
    return targetClassDescriptor != ownerClassDescriptor
}

inline fun ClassDescriptor.findFirstFunction(name: String, predicate: (CallableMemberDescriptor) -> Boolean) =
        unsubstitutedMemberScope.findFirstFunction(name, predicate)

inline fun MemberScope.findFirstFunction(name: String, predicate: (CallableMemberDescriptor) -> Boolean) =
        getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND).first(predicate)
