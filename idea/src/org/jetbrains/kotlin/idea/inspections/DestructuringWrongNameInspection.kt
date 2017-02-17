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

import com.intellij.codeInsight.daemon.impl.quickfix.RenameElementFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtVisitorVoid

class DestructuringWrongNameInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration) {
                super.visitDestructuringDeclaration(destructuringDeclaration)

                val initializer = destructuringDeclaration.initializer ?: return
                val type = initializer.analyze().getType(initializer) ?: return

                val classDescriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return

                val primaryParameterNames = classDescriptor.constructors
                                                .firstOrNull { it.isPrimary }
                                                ?.valueParameters
                                                ?.map { it.name.asString() } ?: return

                destructuringDeclaration.entries.forEachIndexed { entryIndex, entry ->
                    val variableName = entry.name
                    if (variableName != primaryParameterNames.getOrNull(entryIndex)) {
                        for ((parameterIndex, parameterName) in primaryParameterNames.withIndex()) {
                            if (parameterIndex == entryIndex) continue
                            if (variableName == parameterName) {
                                val fix = primaryParameterNames.getOrNull(entryIndex)?.let { RenameElementFix(entry, it) }
                                holder.registerProblem(
                                        entry,
                                        "Variable name '$variableName' matches the name of a different component",
                                        *listOfNotNull(fix).toTypedArray())
                                break
                            }
                        }
                    }
                }
            }
        }
    }
}