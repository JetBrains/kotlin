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
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.descriptors.isSealed
import org.jetbrains.kotlin.idea.KotlinBundle


class KotlinSealedInheritorsInJavaInspection : LocalInspectionTool() {
    companion object {
        private fun PsiClass.listSealedParentReferences(): List<PsiReference> {
            if (this is PsiAnonymousClass && baseClassType.isKotlinSealed())
                return listOf(baseClassReference)

            val sealedBaseClasses = extendsList?.listSealedMembers()
            val sealedBaseInterfaces = implementsList?.listSealedMembers()

            return sealedBaseClasses.orEmpty() + sealedBaseInterfaces.orEmpty()
        }

        private fun PsiReferenceList.listSealedMembers(): List<PsiReference>? = referencedTypes
            ?.filter { it.isKotlinSealed() }
            ?.mapNotNull { it as? PsiClassReferenceType }
            ?.map { it.reference }

        private fun PsiClassType.isKotlinSealed(): Boolean = resolve()?.isKotlinSealed() == true

        private fun PsiClass.isKotlinSealed(): Boolean = this is KtUltraLightClass && getDescriptor()?.isSealed() == true

        private val PsiClass.abstractionTypeName: String
            get() = if (isInterface) "interface" else "class"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitClass(aClass: PsiClass?) {
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