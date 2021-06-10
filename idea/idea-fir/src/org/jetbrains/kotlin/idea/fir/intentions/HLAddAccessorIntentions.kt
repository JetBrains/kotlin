/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import org.jetbrains.kotlin.idea.api.applicator.HLApplicator
import org.jetbrains.kotlin.idea.api.applicator.HLApplicatorInput
import org.jetbrains.kotlin.idea.api.applicator.applicator
import org.jetbrains.kotlin.idea.fir.api.AbstractHLIntention
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicabilityRange
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicatorInputProvider
import org.jetbrains.kotlin.idea.fir.api.applicator.applicabilityTarget
import org.jetbrains.kotlin.idea.fir.api.applicator.inputProvider
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.idea.intentions.AbstractAddAccessorsIntention
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier

abstract class HLAddAccessorIntention(private val addGetter: Boolean, private val addSetter: Boolean) :
    AbstractHLIntention<KtProperty, HLApplicatorInput.Empty>(KtProperty::class, applicator(addGetter, addSetter)) {
    override val applicabilityRange: HLApplicabilityRange<KtProperty> = applicabilityTarget { ktProperty ->
        if (ktProperty.hasInitializer()) ktProperty.nameIdentifier else ktProperty
    }

    override val inputProvider: HLApplicatorInputProvider<KtProperty, HLApplicatorInput.Empty> = inputProvider { ktProperty ->
        val symbol = ktProperty.getVariableSymbol() as? KtPropertySymbol ?: return@inputProvider null
        if (symbol.containsAnnotation(JVM_FIELD_CLASS_ID)) return@inputProvider null

        HLApplicatorInput.Empty
    }

    companion object {
        private val JVM_FIELD_CLASS_ID = ClassId.topLevel(JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME)

        private fun applicator(addGetter: Boolean, addSetter: Boolean): HLApplicator<KtProperty, HLApplicatorInput.Empty> = applicator {
            familyAndActionName(AbstractAddAccessorsIntention.createFamilyName(addGetter, addSetter))

            isApplicableByPsi { ktProperty ->
                if (ktProperty.isLocal || ktProperty.hasDelegate() ||
                    ktProperty.containingClass()?.isInterface() == true ||
                    ktProperty.containingClassOrObject?.hasExpectModifier() == true ||
                    ktProperty.hasModifier(KtTokens.ABSTRACT_KEYWORD) ||
                    ktProperty.hasModifier(KtTokens.LATEINIT_KEYWORD) ||
                    ktProperty.hasModifier(KtTokens.CONST_KEYWORD)
                ) {
                    return@isApplicableByPsi false
                }

                if (ktProperty.typeReference == null && !ktProperty.hasInitializer()) return@isApplicableByPsi false
                if (addSetter && (!ktProperty.isVar || ktProperty.setter != null)) return@isApplicableByPsi false
                if (addGetter && ktProperty.getter != null) return@isApplicableByPsi false

                true
            }

            applyTo { ktProperty, _, _, editor ->
                AbstractAddAccessorsIntention.applyTo(ktProperty, editor, addGetter, addSetter)
            }
        }
    }
}

class HLAddGetterAndSetterIntention : HLAddAccessorIntention(addGetter = true, addSetter = true), LowPriorityAction
class HLAddGetterIntention : HLAddAccessorIntention(addGetter = true, addSetter = false)
class HLAddSetterIntention : HLAddAccessorIntention(addGetter = false, addSetter = true)