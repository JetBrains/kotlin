/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.sealedSubClassToObject

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtPsiFactory.*
import org.jetbrains.kotlin.psi.KtPsiFactory.CallableBuilder.Target.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class GenerateIdentityEqualsFix : LocalQuickFix {
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val klass = descriptor.psiElement.getParentOfType<KtClass>(false) ?: return
        val factory = KtPsiFactory(klass)

        val equalsFunction = factory.createFunction(
            CallableBuilder(FUNCTION).apply {
                modifier(KtTokens.OVERRIDE_KEYWORD.value)
                typeParams()
                name("equals")
                param("other", "Any?")
                returnType("Boolean")
                blockBody("return this === other")
            }.asString()
        )
        klass.addDeclaration(equalsFunction)

        val hashCodeFunction = factory.createFunction(
            CallableBuilder(FUNCTION).apply {
                modifier(KtTokens.OVERRIDE_KEYWORD.value)
                typeParams()
                name("hashCode")
                returnType("Int")
                blockBody("return System.identityHashCode(this)")
            }.asString()
        )
        klass.addDeclaration(hashCodeFunction)
    }

    override fun getFamilyName() = "Generate equals & hashCode by identity"
}