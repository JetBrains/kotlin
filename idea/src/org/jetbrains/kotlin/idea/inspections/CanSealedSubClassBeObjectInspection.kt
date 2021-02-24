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
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.getModalityFromDescriptor
import org.jetbrains.kotlin.idea.quickfix.sealedSubClassToObject.ConvertSealedSubClassToObjectFix
import org.jetbrains.kotlin.idea.quickfix.sealedSubClassToObject.GenerateIdentityEqualsFix
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.util.OperatorNameConventions

class CanSealedSubClassBeObjectInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitClass(klass: KtClass) {
                if (!klass.hasModifier(KtTokens.SEALED_KEYWORD)) return
                if (klass.getModalityFromDescriptor() != KtTokens.SEALED_KEYWORD) return

                val candidates = klass.getSubclasses()
                    .withEmptyConstructors()
                    .thatHasNoClassModifiers()
                    .thatAreFinal()
                    .thatHasNoTypeParameters()
                    .thatHasNoInnerClasses()
                    .thatHasNoCompanionObjects()
                    .thatHasNoStateOrEquals()
                if (candidates.isEmpty() || !klass.hasNoStateOrEquals() || !klass.baseClassHasNoStateOrEquals()) return

                candidates.forEach { reportPossibleObject(it) }
            }

            private fun reportPossibleObject(klass: KtClass) {
                val keyword = klass.getClassOrInterfaceKeyword() ?: return
                holder.registerProblem(
                    keyword,
                    KotlinBundle.message("sealed.sub.class.has.no.state.and.no.overridden.equals"),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    ConvertSealedSubClassToObjectFix(),
                    GenerateIdentityEqualsFix()
                )
            }
        }
    }

    private fun KtClass.getSubclasses(): List<KtLightClassImpl> {
        return HierarchySearchRequest(this, this.useScope, false)
            .searchInheritors().filterIsInstance<KtLightClassImpl>()
    }

    private fun List<KtLightClassImpl>.withEmptyConstructors(): List<KtClass> {
        return map { it.kotlinOrigin }.asSequence().filterIsInstance<KtClass>()
            .filter { it.primaryConstructorParameters.isEmpty() }
            .filter { klass -> klass.secondaryConstructors.all { cons -> cons.valueParameters.isEmpty() } }.toList()
    }

    private fun List<KtClass>.thatHasNoClassModifiers(): List<KtClass> {
        return filter { klass ->
            val modifierList = klass.modifierList ?: return@filter true
            CLASS_MODIFIERS.none { modifierList.hasModifier(it) }
        }
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

    private fun List<KtClass>.thatHasNoStateOrEquals(): List<KtClass> {
        return filter { it.hasNoStateOrEquals() }
    }

    private tailrec fun KtClass.baseClassHasNoStateOrEquals(): Boolean {
        val descriptor = resolveToDescriptorIfAny() ?: return false
        val superDescriptor = descriptor.getSuperClassNotAny() ?: return true // No super class -- no state
        val superClass = DescriptorToSourceUtils.descriptorToDeclaration(superDescriptor) as? KtClass ?: return false
        if (!superClass.hasNoStateOrEquals()) return false
        return superClass.baseClassHasNoStateOrEquals()
    }

    private fun KtClass.hasNoStateOrEquals(): Boolean {
        if (primaryConstructor?.valueParameters?.isNotEmpty() == true) return false
        val body = body
        return body == null || run {
            val declarations = body.declarations
            declarations.asSequence().filterIsInstance<KtProperty>().none { property ->
                // Simplified "backing field required"
                when {
                    property.isAbstract() -> false
                    property.initializer != null -> true
                    property.delegate != null -> false
                    !property.isVar -> property.getter == null
                    else -> property.getter == null || property.setter == null
                }
            } && declarations.asSequence().filterIsInstance<KtNamedFunction>().none { function ->
                val name = function.name
                val valueParameters = function.valueParameters
                val noTypeParameters = function.typeParameters.isEmpty()
                noTypeParameters && (name == EQUALS && valueParameters.size == 1 || name == HASH_CODE && valueParameters.isEmpty())
            }
        }
    }

    private fun KtClass.hasNoInnerClass(): Boolean {
        val internalClasses = body
            ?.declarations
            ?.filterIsInstance<KtClass>() ?: return true

        return internalClasses.none { klass -> klass.isInner() }
    }

    companion object {
        val EQUALS = OperatorNameConventions.EQUALS.asString()

        const val HASH_CODE = "hashCode"

        val CLASS_MODIFIERS = listOf(
            KtTokens.ANNOTATION_KEYWORD,
            KtTokens.DATA_KEYWORD,
            KtTokens.ENUM_KEYWORD,
            KtTokens.INNER_KEYWORD,
            KtTokens.SEALED_KEYWORD,
        )
    }
}
