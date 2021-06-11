/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class RemoveNullableFix(element: KtNullableType, private val typeOfError: NullableKind) :
    KotlinPsiOnlyQuickFixAction<KtNullableType>(element) {
    enum class NullableKind(val message: String) {
        REDUNDANT(KotlinBundle.message("remove.redundant")),
        SUPERTYPE(KotlinBundle.message("text.remove.question")),
        USELESS(KotlinBundle.message("remove.useless")),
        PROPERTY(KotlinBundle.message("make.not.nullable"))
    }

    override fun getFamilyName() = KotlinBundle.message("text.remove.question")

    override fun getText() = typeOfError.message

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val type = element.innerType ?: error("No inner type ${element.text}, should have been rejected in createFactory()")
        element.replace(type)
    }

    companion object {
        val removeForRedundant = createFactory(NullableKind.REDUNDANT)
        val removeForSuperType = createFactory(NullableKind.SUPERTYPE)
        val removeForUseless = createFactory(NullableKind.USELESS)
        val removeForLateInitProperty = createFactory(NullableKind.PROPERTY)

        private fun createFactory(typeOfError: NullableKind): QuickFixesPsiBasedFactory<KtElement> {
            return quickFixesPsiBasedFactory { e ->
                when (typeOfError) {
                    NullableKind.REDUNDANT, NullableKind.SUPERTYPE, NullableKind.USELESS -> {
                        val nullType: KtNullableType? = when (e) {
                            is KtTypeReference -> e.typeElement as? KtNullableType
                            else -> e.getNonStrictParentOfType()
                        }
                        if (nullType?.innerType == null) return@quickFixesPsiBasedFactory emptyList()
                        listOf(RemoveNullableFix(nullType, typeOfError))
                    }
                    NullableKind.PROPERTY -> {
                        val property = e as? KtProperty ?: return@quickFixesPsiBasedFactory emptyList()
                        val typeReference = property.typeReference ?: return@quickFixesPsiBasedFactory emptyList()
                        val typeElement = typeReference.typeElement as? KtNullableType ?: return@quickFixesPsiBasedFactory emptyList()
                        if (typeElement.innerType == null) return@quickFixesPsiBasedFactory emptyList()
                        listOf(RemoveNullableFix(typeElement, NullableKind.PROPERTY))
                    }
                }
            }
        }
    }
}
