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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.JetReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.JetType

fun JetCallableDeclaration.setType(type: JetType) {
    if (type.isError()) return
    val typeReference = JetPsiFactory(getProject()).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))
    setTypeReference(typeReference)
    ShortenReferences.DEFAULT.process(getTypeReference()!!)
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
    val target = reference?.resolveToDescriptors(context)?.firstOrNull() as? ValueParameterDescriptor? ?: return false
    return context[BindingContext.AUTO_CREATED_IT, target]
}
