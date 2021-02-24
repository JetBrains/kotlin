/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.applicators

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.idea.fir.api.applicator.applicabilityTarget
import org.jetbrains.kotlin.idea.refactoring.safeDelete.removeOverrideModifier
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtModifierListOwner

object ApplicabilityRanges {
    val SELF = applicabilityTarget<PsiElement> { it }

    val CALLABLE_RETURN_TYPE = applicabilityTarget<KtCallableDeclaration> { decalration ->
        decalration.typeReference?.typeElement
    }

    val VISIBILITY_MODIFIER = modifier(KtTokens.VISIBILITY_MODIFIERS)

    fun modifier(tokens: TokenSet) = applicabilityTarget<KtModifierListOwner> { declaration ->
        declaration.modifierList?.getModifier(tokens)
    }
}