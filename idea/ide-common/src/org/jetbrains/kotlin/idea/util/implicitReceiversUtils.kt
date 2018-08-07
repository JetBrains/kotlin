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

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.DslMarkerUtils
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import java.util.*
import kotlin.collections.LinkedHashSet

fun LexicalScope.getImplicitReceiversWithInstance(excludeShadowedByDslMarkers: Boolean = false): Collection<ReceiverParameterDescriptor> =
    getImplicitReceiversWithInstanceToExpression(excludeShadowedByDslMarkers).keys

interface ReceiverExpressionFactory {
    val isImmediate: Boolean
    val expressionText: String
    fun createExpression(psiFactory: KtPsiFactory, shortThis: Boolean = true): KtExpression
}

fun LexicalScope.getFactoryForImplicitReceiverWithSubtypeOf(receiverType: KotlinType): ReceiverExpressionFactory? {
    return getImplicitReceiversWithInstanceToExpression()
            .entries
            .firstOrNull { (receiverDescriptor, _) ->
                receiverDescriptor.type.isSubtypeOf(receiverType)
            }
            ?.value
}

fun LexicalScope.getImplicitReceiversWithInstanceToExpression(
    excludeShadowedByDslMarkers: Boolean = false
): Map<ReceiverParameterDescriptor, ReceiverExpressionFactory?> {
    val allReceivers = getImplicitReceiversHierarchy()
    // we use a set to workaround a bug with receiver for companion object present twice in the result of getImplicitReceiversHierarchy()
    val receivers = LinkedHashSet(
        if (excludeShadowedByDslMarkers) {
            allReceivers - allReceivers.shadowedByDslMarkers()
        } else {
            allReceivers
        }
    )

    val outerDeclarationsWithInstance = LinkedHashSet<DeclarationDescriptor>()
    var current: DeclarationDescriptor? = ownerDescriptor
    while (current != null) {
        if (current is PropertyAccessorDescriptor) {
            current = current.correspondingProperty
        }
        outerDeclarationsWithInstance.add(current)

        val classDescriptor = current as? ClassDescriptor
        if (classDescriptor != null && !classDescriptor.isInner && !DescriptorUtils.isLocal(classDescriptor)) break

        current = current.containingDeclaration
    }

    val result = LinkedHashMap<ReceiverParameterDescriptor, ReceiverExpressionFactory?>()
    for ((index, receiver) in receivers.withIndex()) {
        val owner = receiver.containingDeclaration
        if (owner is ScriptDescriptor) {
            result.put(receiver, null)
            continue
        }
        val (expressionText, isImmediateThis) = if (owner in outerDeclarationsWithInstance) {
            val thisWithLabel = thisQualifierName(receiver)?.let { "this@${it.render()}" }
            if (index == 0)
                (thisWithLabel ?: "this") to true
            else
                thisWithLabel to false
        }
        else if (owner is ClassDescriptor && owner.kind.isSingleton) {
            IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(owner) to false
        }
        else {
            continue
        }
        val factory = if (expressionText != null)
            object : ReceiverExpressionFactory {
                override val isImmediate = isImmediateThis
                override val expressionText: String get() = expressionText
                override fun createExpression(psiFactory: KtPsiFactory, shortThis: Boolean): KtExpression {
                    return psiFactory.createExpression(if (shortThis && isImmediateThis) "this" else expressionText)
                }
            }
        else
            null
        result.put(receiver, factory)
    }
    return result
}

private fun thisQualifierName(receiver: ReceiverParameterDescriptor): Name? {
    val descriptor = receiver.containingDeclaration
    val name = descriptor.name
    if (!name.isSpecial) return name

    val functionLiteral = DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? KtFunctionLiteral
    return functionLiteral?.findLabelAndCall()?.first
}

private fun List<ReceiverParameterDescriptor>.shadowedByDslMarkers(): Set<ReceiverParameterDescriptor> {
    val typesByDslScopes = LinkedHashMap<FqName, MutableList<ReceiverParameterDescriptor>>()

    this.mapNotNull { receiver ->
        val dslMarkers = DslMarkerUtils.extractDslMarkerFqNames(receiver.value).all()
        (receiver to dslMarkers).takeIf { dslMarkers.isNotEmpty() }
    }.forEach { (v, dslMarkers) -> dslMarkers.forEach { typesByDslScopes.getOrPut(it, { mutableListOf() }) += v } }

    val shadowedDslReceivers = mutableSetOf<ReceiverParameterDescriptor>()
    typesByDslScopes.flatMapTo(shadowedDslReceivers) { (_, v) -> v.asSequence().drop(1).asIterable() }

    return shadowedDslReceivers
}