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

import org.jetbrains.jet.lang.psi.JetReferenceExpression
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.JetTreeVisitorVoid
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import com.intellij.util.containers.HashMap
import org.jetbrains.jet.lang.psi.JetUserType
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression

object JetFileReferencesResolver {
    fun resolve(
            file: JetFile,
            elements: Iterable<JetElement>? = null,
            visitReceivers: Boolean = true,
            visitShortNames: Boolean = true
    ): Map<JetReferenceExpression, BindingContext> {
        val visitor = ResolveAllReferencesVisitor(file, visitReceivers, visitShortNames)
        if (elements != null) {
            elements.forEach { it.accept(visitor) }
        }
        else {
            file.accept(visitor)
        }
        return visitor.result
    }

    private class ResolveAllReferencesVisitor(file: JetFile, val visitReceivers: Boolean, val visitShortNames: Boolean) : JetTreeVisitorVoid() {
        private val resolveSession = AnalyzerFacadeWithCache.getLazyResolveSessionForFile(file)
        private val resolveMap = HashMap<JetReferenceExpression, BindingContext>()

        public val result: Map<JetReferenceExpression, BindingContext> = resolveMap

        [suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")]
        override fun visitUserType(userType: JetUserType) {
            if (visitReceivers) {
                userType.acceptChildren(this)
            }

            if (visitShortNames || userType.getQualifier() != null) {
                val referenceExpression = userType.getReferenceExpression()
                if (referenceExpression != null) {
                    resolveMap[referenceExpression] = resolveSession.resolveToElement(referenceExpression)
                }
            }
        }

        override fun visitDotQualifiedExpression(expression: JetDotQualifiedExpression) {
            if (visitReceivers) {
                expression.acceptChildren(this)
            }

            val referenceExpression = expression.getSelectorExpression()?.referenceExpression()
            if (referenceExpression != null) {
                resolveMap[referenceExpression] = resolveSession.resolveToElement(referenceExpression)
            }
        }

        override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
            if (visitShortNames) {
                resolveMap[expression] = resolveSession.resolveToElement(expression)
            }
        }
    }
}

fun JetExpression.referenceExpression(): JetReferenceExpression? =
        (if (this is JetCallExpression) getCalleeExpression() else this) as? JetReferenceExpression