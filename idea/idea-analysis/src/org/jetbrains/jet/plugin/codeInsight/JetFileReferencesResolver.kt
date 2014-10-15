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
import com.intellij.util.containers.HashMap
import org.jetbrains.jet.lang.psi.JetUserType
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import java.util.Collections
import org.jetbrains.jet.plugin.caches.resolve.getLazyResolveSession
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor

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
        private val resolveSession = file.getLazyResolveSession()
        private val resolveMap = HashMap<JetReferenceExpression, BindingContext>()

        public val result: Map<JetReferenceExpression, BindingContext> = resolveMap

        override fun visitUserType(userType: JetUserType) {
            if (resolveQualifiers) {
                userType.acceptChildren(this)
            }

            if (resolveShortNames || userType.getQualifier() != null) {
                val referenceExpression = userType.getReferenceExpression()
                if (referenceExpression != null) {
                    resolveMap[referenceExpression] = resolveSession.resolveToElement(referenceExpression)
                }
            }
        }

        private fun JetExpression.isReceiver(): Boolean {
            val parent = getParent()
            if (parent !is JetQualifiedExpression) return false
            if (parent.getReceiverExpression() == this) return true

            val parentParent = parent.getParent()
            return parentParent is JetQualifiedExpression && parentParent.getReceiverExpression() == parent
        }

        override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
            val context = resolveSession.resolveToElement(expression)
            val descriptor = context[BindingContext.REFERENCE_TARGET, expression]
            if ((descriptor is ClassDescriptor || descriptor is PackageViewDescriptor) && expression.isReceiver()) {
                if (!resolveQualifiers) return
            }
            if (!resolveShortNames) return

            resolveMap[expression] = context
        }
    }
}

fun JetExpression.referenceExpression(): JetReferenceExpression? =
        (if (this is JetCallExpression) getCalleeExpression() else this) as? JetReferenceExpression
