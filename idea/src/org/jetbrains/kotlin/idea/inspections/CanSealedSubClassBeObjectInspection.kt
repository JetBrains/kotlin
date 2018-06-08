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
package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.asJava.classes.KtLightClassImpl
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.getModalityFromDescriptor
import org.jetbrains.kotlin.idea.quickfix.sealedSubClassToObject.ConvertSealedSubClassToObjectFix
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny

class CanSealedSubClassBeObjectInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitClass(klass: KtClass) {
                if (!klass.hasModifier(KtTokens.SEALED_KEYWORD)) return
                if (klass.getModalityFromDescriptor() != KtTokens.SEALED_KEYWORD) return

                val candidates = klass.getSubclasses()
                    .withEmptyConstructors()
                    .thatAreFinal()
                    .thatHasNoTypeParameters()
                    .thatHasNoInnerClasses()
                    .thatHasNoCompanionObjects()
                    .thatHasNoState()
                if (candidates.isEmpty() || !klass.hasNoState() || !klass.baseClassHasNoState()) return

                candidates.forEach { reportPossibleObject(it) }
            }

            private fun reportPossibleObject(klass: KtClass) {
                val keyword = klass.getClassOrInterfaceKeyword() ?: return
                holder.registerProblem(
                    keyword,
                    "Sealed Sub-class should be changed To Object",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    ConvertSealedSubClassToObjectFix()
                )
            }
        }
    }

    private tailrec fun KtClass.baseClassHasNoState(): Boolean {
        val descriptor = resolveToDescriptorIfAny() ?: return false
        val superDescriptor = descriptor.getSuperClassNotAny() ?: return true // No super class -- no state
        val superClass = DescriptorToSourceUtils.descriptorToDeclaration(superDescriptor) as? KtClass ?: return false
        if (!superClass.hasNoState()) return false
        return superClass.baseClassHasNoState()
    }

    private fun KtClass.getSubclasses(): List<KtLightClassImpl> {
        return HierarchySearchRequest(this, this.useScope, false)
            .searchInheritors().filterIsInstance<KtLightClassImpl>()
    }

    private fun List<KtLightClassImpl>.withEmptyConstructors(): List<KtClass> {
        return map { it.kotlinOrigin }.filterIsInstance<KtClass>()
            .filter { it.primaryConstructorParameters.isEmpty() }
            .filter { klass -> klass.secondaryConstructors.all { cons -> cons.valueParameters.isEmpty() } }
    }

    private fun List<KtClass>.thatHasNoCompanionObjects(): List<KtClass> {
        return filter { klass -> klass.companionObjects.isEmpty() }
    }

    private fun List<KtClass>.thatAreFinal(): List<KtClass> {
        return filter { klass -> klass.getModalityFromDescriptor() == KtTokens.FINAL_KEYWORD }
    }

    private fun List<KtClass>.thatHasNoTypeParameters(): List<KtClass> {
        return filter { klass -> klass.typeParameters.isEmpty() }
    }

    private fun List<KtClass>.thatHasNoInnerClasses(): List<KtClass> {
        return filter { klass -> klass.hasNoInnerClass() }
    }

    private fun List<KtClass>.thatHasNoState(): List<KtClass> {
        return filter { it.hasNoState() }
    }

    private fun KtClass.hasNoState(): Boolean {
        if (primaryConstructor?.valueParameters?.isNotEmpty() == true) return false
        val body = getBody()
        return body == null || run {
            val properties = body.declarations.filterIsInstance<KtProperty>()
            properties.none { property ->
                // Simplified "backing field required"
                when {
                    property.isAbstract() -> false
                    property.initializer != null -> true
                    property.delegate != null -> false
                    !property.isVar -> property.getter == null
                    else -> property.getter == null || property.setter == null
                }
            }
        }
    }

    private fun KtClass.hasNoInnerClass(): Boolean {
        val internalClasses = getBody()
            ?.declarations
            ?.filterIsInstance<KtClass>() ?: return true

        return internalClasses.none { klass -> klass.isInner() }
    }
}
