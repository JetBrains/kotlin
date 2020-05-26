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

package org.jetbrains.kotlin.idea.inspections.collections

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.getFunctionalClassKind
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.ReceiverKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedLambdaAtom
import org.jetbrains.kotlin.resolve.calls.model.unwrap
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.resolve.calls.tower.NewResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.tower.receiverValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun KotlinType.isFunctionOfAnyKind() = constructor.declarationDescriptor?.getFunctionalClassKind() != null

fun KotlinType?.isMap(builtIns: KotlinBuiltIns): Boolean {
    val classDescriptor = this?.constructor?.declarationDescriptor as? ClassDescriptor ?: return false
    return classDescriptor.name.asString().endsWith("Map") && classDescriptor.isSubclassOf(builtIns.map)
}

fun KotlinType?.isIterable(builtIns: KotlinBuiltIns): Boolean {
    val classDescriptor = this?.constructor?.declarationDescriptor as? ClassDescriptor ?: return false
    val className = classDescriptor.name.asString()
    // First two lines are just to make things faster
    return className.endsWith("List") && classDescriptor.isSubclassOf(builtIns.list)
            || className.endsWith("Set") && classDescriptor.isSubclassOf(builtIns.set)
            || classDescriptor.isSubclassOf(builtIns.iterable)
}

fun KtCallExpression.isCalling(fqName: FqName, context: BindingContext = analyze(BodyResolveMode.PARTIAL)): Boolean {
    return isCalling(listOf(fqName), context)
}

fun KtCallExpression.isCalling(fqNames: List<FqName>, context: BindingContext = analyze(BodyResolveMode.PARTIAL)): Boolean {
    val calleeText = calleeExpression?.text ?: return false
    val fqName = fqNames.firstOrNull { fqName -> fqName.shortName().asString() == calleeText } ?: return false
    return getResolvedCall(context)?.isCalling(fqName) == true
}

fun ResolvedCall<out CallableDescriptor>.isCalling(fqName: FqName): Boolean {
    return resultingDescriptor.fqNameSafe == fqName
}

fun ResolvedCall<*>.hasLastFunctionalParameterWithResult(context: BindingContext, predicate: (KotlinType) -> Boolean): Boolean {
    val lastParameter = resultingDescriptor.valueParameters.lastOrNull() ?: return false
    val lastArgument = valueArguments[lastParameter]?.arguments?.singleOrNull() ?: return false
    if (this is NewResolvedCallImpl<*>) {
        // TODO: looks like hack
        resolvedCallAtom.subResolvedAtoms?.firstOrNull { it is ResolvedLambdaAtom }.safeAs<ResolvedLambdaAtom>()?.let { lambdaAtom ->
            return lambdaAtom.unwrap().resultArgumentsInfo!!.nonErrorArguments.filterIsInstance<ReceiverKotlinCallArgument>().all {
                val type = it.receiverValue?.type ?: return@all false
                predicate(type)
            }
        }
    }

    val functionalType = lastArgument.getArgumentExpression()?.getType(context) ?: return false
    // Both Function & KFunction must pass here
    if (!functionalType.isFunctionOfAnyKind()) return false
    val resultType = functionalType.arguments.lastOrNull()?.type ?: return false
    return predicate(resultType)
}

fun KtCallExpression.implicitReceiver(context: BindingContext): ImplicitReceiver? {
    return getResolvedCall(context)?.getImplicitReceiverValue()
}

fun KtCallExpression.receiverType(context: BindingContext): KotlinType? {
    return (getQualifiedExpressionForSelector())?.receiverExpression?.getResolvedCall(context)?.resultingDescriptor?.returnType
        ?: implicitReceiver(context)?.type
}