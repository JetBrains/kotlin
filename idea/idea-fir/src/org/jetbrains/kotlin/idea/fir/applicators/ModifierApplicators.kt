/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.applicators

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicatorInput
import org.jetbrains.kotlin.idea.fir.api.applicator.applicator
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken

import org.jetbrains.kotlin.psi.KtModifierListOwner

object ModifierApplicators {
    fun removeModifierApplicator(modifier: TokenSet, familyName: () -> String) = applicator<KtModifierListOwner, Modifier> {
        familyName(familyName)
        actionName { _, (modifier) -> KotlinBundle.message("remove.0.modifier", modifier.value) }

        isApplicableByPsi { modifierOwner ->
            modifierOwner.modifierList?.getModifier(modifier) != null
        }

        applyTo { modifierOwner, (modifier) ->
            runWriteAction {
                modifierOwner.removeModifier(modifier)
            }
        }
    }

    class Modifier(val modifier: KtModifierKeywordToken) : HLApplicatorInput {
        override fun isValidFor(psi: PsiElement): Boolean {
            if (psi !is KtModifierListOwner) return false
            return psi.hasModifier(modifier)
        }

        operator fun component1() = modifier
    }
}