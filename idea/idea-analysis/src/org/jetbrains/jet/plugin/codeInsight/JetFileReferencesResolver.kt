/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.codeInsight

import org.jetbrains.kotlin.psi.JetReferenceExpression
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetTreeVisitorVoid
import org.jetbrains.kotlin.psi.JetUserType
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import java.util.Collections
import org.jetbrains.jet.plugin.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.psi.JetQualifiedExpression
import java.util.LinkedHashMap

object JetFileReferencesResolver {
    fun resolve(
            element: JetElement,
            resolveQualifiers: Boolean = true,
            resolveShortNames: Boolean = true
    ): Map<JetReferenceExpression, BindingContext> {
        return (element.getContainingFile() as? JetFile)?.let { file ->
            resolve(file, listOf(element), resolveQualifiers, resolveShortNames)
        } ?: Collections.emptyMap()
    }

    fun resolve(
            file: JetFile,
            elements: Iterable<JetElement>? = null,
            resolveQualifiers: Boolean = true,
            resolveShortNames: Boolean = true
    ): Map<JetReferenceExpression, BindingContext> {
        val visitor = ResolveAllReferencesVisitor(file, resolveQualifiers, resolveShortNames)
        if (elements != null) {
            elements.forEach { it.accept(visitor) }
        }
        else {
            file.accept(visitor)
        }
        return visitor.result
    }

    private class ResolveAllReferencesVisitor(file: JetFile, val resolveQualifiers: Boolean, val resolveShortNames: Boolean) : JetTreeVisitorVoid() {
        private val resolutionFacade = file.getResolutionFacade()
        private val resolveMap = LinkedHashMap<JetReferenceExpression, BindingContext>()

        public val result: Map<JetReferenceExpression, BindingContext> = resolveMap

        override fun visitUserType(userType: JetUserType) {
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

        override fun visitQualifiedExpression(expression: JetQualifiedExpression) {
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

        override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
            if (resolveShortNames) {
                resolveMap[expression] = resolutionFacade.analyze(expression)
            }
        }
    }
}

fun JetExpression.referenceExpression(): JetReferenceExpression? =
        (if (this is JetCallExpression) getCalleeExpression() else this) as? JetReferenceExpression
