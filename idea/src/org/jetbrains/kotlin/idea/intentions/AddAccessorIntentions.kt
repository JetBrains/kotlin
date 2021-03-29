/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.idea.util.hasJvmFieldAnnotation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtProperty

abstract class AddAccessorsIntention(
    addGetter: Boolean,
    addSetter: Boolean
) : AbstractAddAccessorsIntention(addGetter, addSetter) {

    override fun applicabilityRange(element: KtProperty): TextRange? {
        if (element.isLocal || element.isAbstract() || element.hasDelegate() ||
            element.hasModifier(KtTokens.LATEINIT_KEYWORD) ||
            element.hasModifier(KtTokens.CONST_KEYWORD) ||
            element.hasJvmFieldAnnotation()
        ) {
            return null
        }
        val descriptor = element.resolveToDescriptorIfAny() as? CallableMemberDescriptor ?: return null
        if (descriptor.isExpect) return null

        val hasInitializer = element.hasInitializer()
        if (element.typeReference == null && !hasInitializer) return null
        if (addSetter && (!element.isVar || element.setter != null)) return null
        if (addGetter && element.getter != null) return null
        return if (hasInitializer) element.nameIdentifier?.textRange else element.textRange
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val property = diagnostic.psiElement as? KtProperty ?: return null
            return if (property.isVar) {
                val getter = property.getter
                val setter = property.setter
                when {
                    getter == null && setter == null -> AddPropertyAccessorsIntention()
                    getter == null -> AddPropertyGetterIntention()
                    else -> AddPropertySetterIntention()
                }
            } else {
                AddPropertyGetterIntention()
            }
        }
    }
}

class AddPropertyAccessorsIntention : AddAccessorsIntention(true, true), LowPriorityAction

class AddPropertyGetterIntention : AddAccessorsIntention(true, false)

class AddPropertySetterIntention : AddAccessorsIntention(false, true)
