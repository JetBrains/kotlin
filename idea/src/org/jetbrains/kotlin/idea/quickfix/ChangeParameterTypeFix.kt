/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeSignatureConfiguration
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinMethodDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinTypeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.runChangeSignature
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

class ChangeParameterTypeFix(element: KtParameter, type: KotlinType) : KotlinQuickFixAction<KtParameter>(element) {
    private val typePresentation = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(type)
    private val typeInfo =
        KotlinTypeInfo(isCovariant = false, text = IdeDescriptorRenderers.SOURCE_CODE_NOT_NULL_TYPE_APPROXIMATION.renderType(type))

    private val containingDeclarationName: String?
    private val isPrimaryConstructorParameter: Boolean

    init {
        val declaration = PsiTreeUtil.getParentOfType(element, KtNamedDeclaration::class.java)
        this.containingDeclarationName = declaration?.name
        this.isPrimaryConstructorParameter = declaration is KtPrimaryConstructor
    }

    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        return containingDeclarationName != null
    }

    override fun getText(): String {
        val element = element ?: return ""
        return if (isPrimaryConstructorParameter)
            "Change parameter '${element.name}' type of primary constructor of class '$containingDeclarationName' to '$typePresentation'"
        else
            "Change parameter '${element.name}' type of function '$containingDeclarationName' to '$typePresentation'"
    }

    override fun getFamilyName() = KotlinBundle.message("change.type.family")

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val function = element.getStrictParentOfType<KtFunction>() ?: return
        val parameterIndex = function.valueParameters.indexOf(element)
        val descriptor = function.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? FunctionDescriptor ?: return
        val configuration = object : KotlinChangeSignatureConfiguration {
            override fun configure(originalDescriptor: KotlinMethodDescriptor) = originalDescriptor.apply {
                parameters[if (receiver != null) parameterIndex + 1 else parameterIndex].currentTypeInfo = typeInfo
            }

            override fun performSilently(affectedFunctions: Collection<PsiElement>) = true
        }
        runChangeSignature(element.project, descriptor, configuration, element, text)
    }
}
