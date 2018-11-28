/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.api

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.kotlin.psi.psiUtil.parents

class IncompatibleAPIJavaVisitor internal constructor(
    private val myHolder: ProblemsHolder,
    private val problemsCache: ProblemsCache
) : JavaElementVisitor() {
    override fun visitDocComment(comment: PsiDocComment) {
        // No references inside doc comment are of interest.
    }

    override fun visitImportList(list: PsiImportList?) {
        // Do not report anything in imports
    }

    override fun visitClass(aClass: PsiClass) {}

    override fun visitReferenceExpression(expression: PsiReferenceExpression) {
        visitReferenceElement(expression)
    }

    override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
        val isInsideImport = reference.element.parents
            .takeWhile { it is PsiJavaCodeReferenceElement || it is PsiImportStatement }
            .any { it is PsiImportStatement }
        if (isInsideImport) {
            return
        }

        ModuleUtilCore.findModuleForPsiElement(reference.element) ?: return
        super.visitReferenceElement(reference)

        val name = reference.referenceName
        if (name == null || !problemsCache.containsWord(name)) {
            return
        }

        val psiMember = reference.resolve() as? PsiMember ?: return
        val problem = findProblem(psiMember, problemsCache) ?: return
        registerProblemForReference(reference, myHolder, problem)
    }

    override fun visitNewExpression(expression: PsiNewExpression) {
        ModuleUtilCore.findModuleForPsiElement(expression) ?: return
        super.visitNewExpression(expression)

        val name = expression.classReference?.referenceName
        if (name == null || !problemsCache.containsWord(name)) {
            return
        }

        val constructor = expression.resolveConstructor()
        if (exitOnNonCompiled(constructor)) return

        val problem = findProblem(constructor, problemsCache)
        if (problem != null) {
            registerProblemForReference(expression.classReference, myHolder, problem)
        }
    }

    override fun visitMethod(method: PsiMethod) {
        ModuleUtilCore.findModuleForPsiElement(method) ?: return
        super.visitMethod(method)

        if (!problemsCache.containsWord(method.name)) {
            return
        }

        (if (!method.isConstructor) AnnotationUtil.findAnnotation(method, CommonClassNames.JAVA_LANG_OVERRIDE) else null) ?: return

        val methods = method.findSuperMethods()
        for (superMethod in methods) {
            if (exitOnNonCompiled(superMethod)) {
                return
            }

            val problem = findProblem(superMethod, problemsCache)
            if (problem != null) {
                registerProblemForElement(method.nameIdentifier, myHolder, problem)
                return
            }
        }
    }

    private fun exitOnNonCompiled(psiElement: PsiElement?): Boolean {
        if (psiElement != null && psiElement !is PsiCompiledElement) {
            if (!ApplicationManager.getApplication().isInternal && !ApplicationManager.getApplication().isUnitTestMode) {
                return true
            }
        }

        return false
    }
}