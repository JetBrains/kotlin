/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiTypeParameterListOwner
import com.intellij.psi.ResolveState
import com.intellij.psi.impl.light.LightTypeParameterListBuilder
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.kotlin.idea.KotlinLanguage

// Used by UastFakeSourceLightMethod
class KotlinLightTypeParameterListBuilder(private val owner: PsiTypeParameterListOwner) :
    LightTypeParameterListBuilder(owner.manager, KotlinLanguage.INSTANCE) {
    override fun processDeclarations(
        processor: PsiScopeProcessor,
        state: ResolveState,
        lastParent: PsiElement?,
        place: PsiElement
    ): Boolean {
        return typeParameters.all { processor.execute(it, state) }
    }

    override fun getParent(): PsiElement = owner
    override fun getContainingFile(): PsiFile = owner.containingFile

    override fun getText(): String? = ""
}
