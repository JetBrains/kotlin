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

package org.jetbrains.kotlin.idea.refactoring

import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.JetScope

import java.util.HashSet

public class NameValidatorImpl(
        private val container: PsiElement,
        private val anchor: PsiElement?,
        private val target: NameValidatorImpl.Target
) : (String) -> Boolean {
    public enum class Target {
        FUNCTIONS_AND_CLASSES,
        PROPERTIES
    }

    override fun invoke(name: String): Boolean {
        val visitedScopes = HashSet<JetScope>()

        var sibling: PsiElement?
        if (anchor != null) {
            sibling = anchor
        }
        else {
            if (container is JetExpression) {
                return checkElement(name, container, visitedScopes)
            }
            sibling = container.getFirstChild()
        }

        while (sibling != null) {
            if (!checkElement(name, sibling, visitedScopes)) return false
            sibling = sibling.getNextSibling()
        }

        return true
    }

    private fun checkElement(name: String, sibling: PsiElement, visitedScopes: MutableSet<JetScope>): Boolean {
        if (sibling !is JetElement) return true

        val context = sibling.analyze(BodyResolveMode.FULL)
        val identifier = Name.identifier(name)

        val result = Ref(true)
        val visitor = object : JetVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                if (result.get()) {
                    element.acceptChildren(this)
                }
            }

            override fun visitExpression(expression: JetExpression) {
                val resolutionScope = expression.getResolutionScope(context, expression.getResolutionFacade())

                if (!visitedScopes.add(resolutionScope)) return

                val noConflict: Boolean
                if (target === Target.PROPERTIES) {
                    noConflict = resolutionScope.getProperties(identifier).isEmpty() && resolutionScope.getLocalVariable(identifier) == null
                }
                else {
                    noConflict = resolutionScope.getFunctions(identifier).isEmpty() && resolutionScope.getClassifier(identifier) == null
                }

                if (!noConflict) {
                    result.set(false)
                    return
                }

                super.visitExpression(expression)
            }
        }
        sibling.accept(visitor)
        return result.get()
    }
}
