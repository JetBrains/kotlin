/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.descriptors.isSealed
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.refactoring.memberInfo.getClassDescriptorIfAny


class KotlinSealedInheritorsInJavaInspection : LocalInspectionTool() {
    companion object {
        private fun PsiClass.listSealedParentReferences(): List<PsiReference> {
            if (this is PsiAnonymousClass && baseClassType.isKotlinSealed())
                return listOf(baseClassReference)

            val sealedBaseClasses = extendsList?.listSealedMembers()
            val sealedBaseInterfaces = implementsList?.listSealedMembers()

            return sealedBaseClasses.orEmpty() + sealedBaseInterfaces.orEmpty()
        }

        private fun PsiReferenceList.listSealedMembers(): List<PsiReference> = referencedTypes
            .filter { it.isKotlinSealed() }
            .mapNotNull { it as? PsiClassReferenceType }
            .map { it.reference }

        private fun PsiClassType.isKotlinSealed(): Boolean = resolve()?.isKotlinSealed() == true

        private fun PsiClass.isKotlinSealed(): Boolean {
            return this is KtLightClass && (getClassDescriptorIfAny()?.isSealed() ?: false)
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitClass(aClass: PsiClass?) {
                if (aClass is PsiTypeParameter) return
                aClass?.listSealedParentReferences()?.forEach {
                    holder.registerProblem(
                        it, KotlinBundle.message("inheritance.of.kotlin.sealed", 0.takeIf { aClass.isInterface } ?: 1),
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }

            override fun visitAnonymousClass(aClass: PsiAnonymousClass?) = visitClass(aClass)
        }
    }
}