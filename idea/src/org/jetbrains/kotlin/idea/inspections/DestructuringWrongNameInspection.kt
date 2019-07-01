/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.daemon.impl.quickfix.RenameElementFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.destructuringDeclarationVisitor

class DestructuringWrongNameInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return destructuringDeclarationVisitor(fun(destructuringDeclaration) {
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
        })
    }
}
