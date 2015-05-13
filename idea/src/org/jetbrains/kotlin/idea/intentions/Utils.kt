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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.references.JetReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.typeRefHelpers.setReceiverTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.JetType

fun JetCallableDeclaration.setType(type: JetType) {
    if (type.isError()) return
    val typeReference = JetPsiFactory(getProject()).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))
    setTypeReference(typeReference)
    ShortenReferences.DEFAULT.process(getTypeReference()!!)
}

fun JetCallableDeclaration.setReceiverType(type: JetType) {
    if (type.isError()) return
    val typeReference = JetPsiFactory(getProject()).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))
    setReceiverTypeReference(typeReference)
    ShortenReferences.DEFAULT.process(getReceiverTypeReference()!!)
}

fun JetContainerNode.description(): String? {
    when (getNode().getElementType()) {
        JetNodeTypes.THEN -> return "if"
        JetNodeTypes.ELSE -> return "else"
        JetNodeTypes.BODY -> {
            when (getParent()) {
                is JetWhileExpression -> return "while"
                is JetDoWhileExpression -> return "do...while"
                is JetForExpression -> return "for"
            }
        }
    }
    return null
}

fun TextRange.containsInside(offset: Int) = getStartOffset() < offset && offset < getEndOffset()

fun isAutoCreatedItUsage(expression: JetSimpleNameExpression): Boolean {
    if (expression.getReferencedName() != "it") return false
    val context = expression.analyze()
    val reference = expression.getReference() as JetReference?
    val target = reference?.resolveToDescriptors(context)?.singleOrNull() as? ValueParameterDescriptor? ?: return false
    return context[BindingContext.AUTO_CREATED_IT, target]
}

fun JetCallableDeclaration.canRemoveTypeSpecificationByVisibility(): Boolean {
    val descriptor = resolveToDescriptor()
    val isOverride = getModifierList()?.hasModifier(JetTokens.OVERRIDE_KEYWORD) ?: false
    if (!isOverride && (descriptor as? DeclarationDescriptorWithVisibility)?.getVisibility()?.isPublicAPI() ?: false) return false
    return true
}

// returns assignment which replaces initializer
fun splitPropertyDeclaration(property: JetProperty): JetBinaryExpression {
    val parent = property.getParent()!!

    val initializer = property.getInitializer()!!

    val explicitTypeToSet = if (property.getTypeReference() != null) null else initializer.analyze().getType(initializer)

    val psiFactory = JetPsiFactory(property)
    var assignment = psiFactory.createExpressionByPattern("$0 = $1", property.getName()!!, initializer)

    assignment = parent.addAfter(assignment, property) as JetBinaryExpression
    parent.addAfter(psiFactory.createNewLine(), property)

    property.setInitializer(null)

    if (explicitTypeToSet != null) {
        property.setType(explicitTypeToSet)
    }

    return assignment
}

val JetQualifiedExpression.callExpression: JetCallExpression?
    get() = getSelectorExpression() as? JetCallExpression

val JetQualifiedExpression.calleeName: String?
    get() = (callExpression?.getCalleeExpression() as? JetSimpleNameExpression)?.getText()

fun JetQualifiedExpression.toResolvedCall(): ResolvedCall<out CallableDescriptor>? {
    val callExpression = callExpression ?: return null
    return callExpression.getResolvedCall(callExpression.analyze()) ?: return null
}

