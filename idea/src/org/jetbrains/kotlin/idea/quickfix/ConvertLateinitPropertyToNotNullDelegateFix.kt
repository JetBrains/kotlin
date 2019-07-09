/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ConvertLateinitPropertyToNotNullDelegateFix(
    property: KtProperty,
    private val type: String
) : KotlinQuickFixAction<KtProperty>(property) {
    override fun getText() = "Convert to notNull delegate"

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val property = element ?: return
        val typeReference = property.typeReference ?: return
        val psiFactory = KtPsiFactory(property)
        property.removeModifier(KtTokens.LATEINIT_KEYWORD)
        val propertyDelegate = psiFactory.createPropertyDelegate(
            psiFactory.createExpression("kotlin.properties.Delegates.notNull<$type>()")
        )
        property.addAfter(propertyDelegate, typeReference)
        property.typeReference = null
        ShortenReferences.DEFAULT.process(property)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtProperty>? {
            val modifierList = diagnostic.psiElement.parent as? KtDeclarationModifierList ?: return null
            val property = modifierList.parent as? KtProperty ?: return null
            if (!property.hasModifier(KtTokens.LATEINIT_KEYWORD) || !property.isVar || property.hasInitializer()) return null
            val typeReference = property.typeReference ?: return null
            val type = property.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, typeReference] ?: return null
            if (!KotlinBuiltIns.isPrimitiveType(type)) return null
            return ConvertLateinitPropertyToNotNullDelegateFix(property, type.toString())
        }
    }
}
