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

package org.jetbrains.kotlin.idea.codeInsight

import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import java.util.Collections
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import java.util.LinkedHashMap

object JetFileReferencesResolver {
    fun resolve(
            element: KtElement,
            resolveQualifiers: Boolean = true,
            resolveShortNames: Boolean = true
    ): Map<KtReferenceExpression, BindingContext> {
        return (element.getContainingFile() as? KtFile)?.let { file ->
            resolve(file, listOf(element), resolveQualifiers, resolveShortNames)
        } ?: Collections.emptyMap()
    }

    fun resolve(
            file: KtFile,
            elements: Iterable<KtElement>? = null,
            resolveQualifiers: Boolean = true,
            resolveShortNames: Boolean = true
    ): Map<KtReferenceExpression, BindingContext> {
        val visitor = ResolveAllReferencesVisitor(file, resolveQualifiers, resolveShortNames)
        if (elements != null) {
            elements.forEach { it.accept(visitor) }
        }
        else {
            file.accept(visitor)
        }
        return visitor.result
    }

    private class ResolveAllReferencesVisitor(file: KtFile, val resolveQualifiers: Boolean, val resolveShortNames: Boolean) : KtTreeVisitorVoid() {
        private val resolutionFacade = file.getResolutionFacade()
        private val resolveMap = LinkedHashMap<KtReferenceExpression, BindingContext>()

        public val result: Map<KtReferenceExpression, BindingContext> = resolveMap

        override fun visitUserType(userType: KtUserType) {
            if (resolveQualifiers) {
                userType.acceptChildren(this)
            }

            if (resolveShortNames || userType.getQualifier() != null) {
                val referenceExpression = userType.getReferenceExpression()
                if (referenceExpression != null) {
                    resolveMap[referenceExpression] = resolutionFacade.analyze(referenceExpression)
                }
            }
        }

        override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
            val receiverExpression = expression.getReceiverExpression()
            if (resolveQualifiers || resolutionFacade.analyze(expression)[BindingContext.QUALIFIER, receiverExpression] == null) {
                receiverExpression.accept(this)
            }

            val referenceExpression = expression.getSelectorExpression()?.referenceExpression()
            if (referenceExpression != null) {
                resolveMap[referenceExpression] = resolutionFacade.analyze(referenceExpression)
            }
            expression.getSelectorExpression()?.accept(this)
        }

        override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
            if (resolveShortNames) {
                resolveMap[expression] = resolutionFacade.analyze(expression)
            }
        }
    }
}

fun KtExpression.referenceExpression(): KtReferenceExpression? =
        (if (this is KtCallExpression) getCalleeExpression() else this) as? KtReferenceExpression
