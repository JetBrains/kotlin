/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class AddTypeAnnotationToValueParameterFix(element: KtParameter) : KotlinQuickFixAction<KtParameter>(element) {
    private val typeNameShort: String?
    val typeName: String?

    init {
        val defaultValue = element.defaultValue
        var type = defaultValue?.getType(defaultValue.analyze(BodyResolveMode.PARTIAL))
        if (type != null && KotlinBuiltIns.isArrayOrPrimitiveArray(type)) {
            if (element.hasModifier(KtTokens.VARARG_KEYWORD)) {
                type = if (KotlinBuiltIns.isPrimitiveArray(type))
                    element.builtIns.getArrayElementType(type)
                else
                    type.arguments.singleOrNull()?.type
            } else if (defaultValue is KtCollectionLiteralExpression) {
                val builtIns = element.builtIns
                val elementType = builtIns.getArrayElementType(type)
                if (KotlinBuiltIns.isPrimitiveType(elementType)) {
                    type = builtIns.getPrimitiveArrayKotlinTypeByPrimitiveKotlinType(elementType)
                }
            }
        }

        typeNameShort = type?.let { IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(it) }
        typeName = type?.let { IdeDescriptorRenderers.SOURCE_CODE.renderType(it) }
    }

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        val element = element ?: return false
        return element.typeReference == null && typeNameShort != null
    }

    override fun getFamilyName() = "Add type annotation"
    override fun getText() = element?.let { "Add type '$typeNameShort' to parameter '${it.name}'" } ?: ""

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        if (typeName != null) {
            element.typeReference = KtPsiFactory(element).createType(typeName)
            ShortenReferences.DEFAULT.process(element)
        }
    }

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): AddTypeAnnotationToValueParameterFix? {
            val element = Errors.VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION.cast(diagnostic).psiElement
            if (element.defaultValue == null) return null
            return AddTypeAnnotationToValueParameterFix(element)
        }
    }
}