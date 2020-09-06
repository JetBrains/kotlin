/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class AddConstructorParameterFromSuperTypeCallFix(
    constructor: KtValueArgumentList,
    private val parameterName: String,
    parameterType: KotlinType
) : KotlinQuickFixAction<KtValueArgumentList>(constructor) {
    private val parameterTypeSourceCode = IdeDescriptorRenderers.SOURCE_CODE_TYPES.renderType(parameterType)

    override fun getText() = KotlinBundle.message("fix.add.constructor.parameter", parameterName)

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val superTypeCallArgList = element ?: return
        val constructorParamList = superTypeCallArgList.containingClass()?.createPrimaryConstructorIfAbsent()?.valueParameterList ?: return
        val psiFactory = KtPsiFactory(superTypeCallArgList)
        val constructorParam = constructorParamList.addParameter(psiFactory.createParameter("$parameterName: $parameterTypeSourceCode"))
        val superTypeCallArg = superTypeCallArgList.addArgument(psiFactory.createArgument(parameterName))
        ShortenReferences.DEFAULT.process(constructorParam)
        editor?.caretModel?.moveToOffset(superTypeCallArg.endOffset)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtValueArgumentList>? {
            val superTypeCallArgList = diagnostic.psiElement as? KtValueArgumentList ?: return null
            val superTypeCall = superTypeCallArgList.parent as? KtSuperTypeCallEntry ?: return null
            val containingClass = superTypeCall.containingClass() ?: return null

            val parameter = DiagnosticFactory.cast(diagnostic, Errors.NO_VALUE_FOR_PARAMETER).a
            val parameterIndex = parameter.index
            if (parameterIndex != superTypeCallArgList.arguments.size) return null
            val parameterName = parameter.name.render()
            val context = superTypeCallArgList.analyze(BodyResolveMode.PARTIAL)
            val constructor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, containingClass]?.safeAs<ClassDescriptor>()
                ?.constructors?.firstOrNull() ?: return null
            if (constructor.valueParameters.any { it.name.asString() == parameterName }) return null
            val superTypeCallParameters = superTypeCall.getResolvedCall(context)?.resultingDescriptor?.valueParameters
            val parameterType = superTypeCallParameters?.getOrNull(parameterIndex)?.type ?: parameter.type

            return AddConstructorParameterFromSuperTypeCallFix(superTypeCallArgList, parameterName, parameterType)
        }
    }
}