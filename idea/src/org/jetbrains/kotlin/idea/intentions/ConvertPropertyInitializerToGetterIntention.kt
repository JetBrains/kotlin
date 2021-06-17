/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.util.hasJvmFieldAnnotation
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.types.isError

class ConvertPropertyInitializerToGetterIntention : SelfTargetingRangeIntention<KtProperty>(
    KtProperty::class.java, KotlinBundle.lazyMessage("convert.property.initializer.to.getter")
) {
    override fun applicabilityRange(element: KtProperty): TextRange? {
        val initializer = element.initializer ?: return null
        val nameIdentifier = element.nameIdentifier ?: return null
        return if (element.getter == null && !element.isExtensionDeclaration() && !element.isLocal && !element.hasJvmFieldAnnotation())
            TextRange(nameIdentifier.startOffset, initializer.endOffset)
        else
            null
    }

    override fun skipProcessingFurtherElementsAfter(element: PsiElement): Boolean {
        // do not work inside lambda's in initializer - they can be too big
        return element is KtDeclaration || super.skipProcessingFurtherElementsAfter(element)
    }

    override fun applyTo(element: KtProperty, editor: Editor?) {
        convertPropertyInitializerToGetter(element, editor)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            return ConvertPropertyInitializerToGetterIntention()
        }

        fun convertPropertyInitializerToGetter(property: KtProperty, editor: Editor?) {
            val type = SpecifyTypeExplicitlyIntention.getTypeForDeclaration(property)
            if (!type.isError) {
                SpecifyTypeExplicitlyIntention.addTypeAnnotation(editor, property, type)
            }

            val initializer = property.initializer!!
            val getter = KtPsiFactory(property).createPropertyGetter(initializer)
            val setter = property.setter

            when {
                setter != null -> property.addBefore(getter, setter)
                property.isVar -> {
                    property.add(getter)
                    val notImplemented = KtPsiFactory(property).createExpression("TODO()")
                    val notImplementedSetter = KtPsiFactory(property).createPropertySetter(notImplemented)
                    property.add(notImplementedSetter)
                }
                else -> property.add(getter)
            }

            property.initializer = null
        }
    }
}
