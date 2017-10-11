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

import com.intellij.codeInspection.*
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.asJava.classes.KtLightClassImpl
import org.jetbrains.kotlin.idea.intentions.declarations.ConvertSealedSubClassToObjectIntention
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtVisitorVoid

class SealedSubClassCanBeObject : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitClass(klass: KtClass) {
                if (!klass.isSealed()) return

                klass.getSubclasses()
                        .withEmptyConstructors()
                        .withoutInheritors()
                        .forEach { reportPossibleObject(it) }
            }

            private fun reportPossibleObject(klass: KtClass) {
                val keyword = klass.getClassOrInterfaceKeyword()?: return
                holder.registerProblem(keyword,
                                       "Sealed Sub-class can be changed To Object",
                                       ProblemHighlightType.WEAK_WARNING,
                                       IntentionWrapper(ConvertSealedSubClassToObjectIntention(), klass.containingKtFile))
            }
        }
    }

    private inline fun KtClass.getSubclasses(): List<KtLightClassImpl> {
        return HierarchySearchRequest(this, this.useScope, false)
                .searchInheritors().filterIsInstance<KtLightClassImpl>()
    }

    private inline fun List<KtLightClassImpl>.withEmptyConstructors(): List<KtClass> {
        return map { it.kotlinOrigin }.filterIsInstance<KtClass>()
                .filter { it.primaryConstructorParameters.isEmpty() }
    }

    private inline fun List<KtClass>.withoutInheritors(): List<KtClass> {
        return filter { HierarchySearchRequest(it, it.useScope, false).searchInheritors().firstOrNull() == null }
    }
}
