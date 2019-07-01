/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.quickfix.crossLanguage

import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.lang.jvm.actions.ChangeParametersRequest
import com.intellij.lang.jvm.actions.ExpectedParameter
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JvmPsiConversionHelper
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.load.java.NOT_NULL_ANNOTATIONS
import org.jetbrains.kotlin.load.java.NULLABLE_ANNOTATIONS
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType

internal class ChangeMethodParameters(
    target: KtNamedFunction,
    val request: ChangeParametersRequest
) : KotlinQuickFixAction<KtNamedFunction>(target) {


    override fun getText(): String {

        val target = element ?: return "<not available>"

        val helper = JvmPsiConversionHelper.getInstance(target.project)

        val parametersString = request.expectedParameters.joinToString(", ", "(", ")") { ep ->
            val kotlinType =
                ep.expectedTypes.firstOrNull()?.theType?.let { helper.convertType(it).resolveToKotlinType(target.getResolutionFacade()) }
            "${ep.semanticNames.firstOrNull() ?: "parameter"}: ${kotlinType?.let {
                IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(it)
            } ?: "<error>"}"
        }

        val shortenParameterString = StringUtil.shortenTextWithEllipsis(parametersString, 30, 5)
        return QuickFixBundle.message("change.method.parameters.text", shortenParameterString)
    }

    override fun getFamilyName(): String = QuickFixBundle.message("change.method.parameters.family")

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean = element != null && request.isValid

    private data class ParameterModification(
        val name: String,
        val ktType: KotlinType
    )

    private fun getParametersModifications(
        target: KtNamedFunction,
        expectedParameters: List<ExpectedParameter>
    ): List<ParameterModification> {
        val helper = JvmPsiConversionHelper.getInstance(target.project)

        return expectedParameters.mapIndexed { index, expectedParameter ->
            val theType = expectedParameter.expectedTypes.firstOrNull()?.theType
            val kotlinType = theType
                ?.let { helper.convertType(it).resolveToKotlinType(target.getResolutionFacade()) }
                ?: ErrorUtils.createErrorType("unknown type")

            ParameterModification(
                expectedParameter.semanticNames.firstOrNull() ?: "param$index", kotlinType
            )
        }
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        if (!request.isValid) return

        val target = element ?: return
        val functionDescriptor = target.resolveToDescriptorIfAny(BodyResolveMode.FULL) ?: return

        target.valueParameterList!!.parameters.forEach { target.valueParameterList!!.removeParameter(it) }
        val parameterActions = getParametersModifications(target, request.expectedParameters)

        val parametersGenerated = parameterActions.let {
            it zip generateParameterList(project, functionDescriptor, it).parameters
        }.toMap()

        for (action in parameterActions) {
            val parameter = parametersGenerated.getValue(action)
            target.valueParameterList!!.addParameter(parameter)
        }

        ShortenReferences.DEFAULT.process(target.valueParameterList!!)
    }

    private fun generateParameterList(
        project: Project,
        functionDescriptor: FunctionDescriptor,
        paramsToAdd: List<ParameterModification>
    ): KtParameterList {
        val newFunctionDescriptor = SimpleFunctionDescriptorImpl.create(
            functionDescriptor.containingDeclaration,
            functionDescriptor.annotations,
            functionDescriptor.name,
            functionDescriptor.kind,
            SourceElement.NO_SOURCE
        ).apply {
            initialize(
                functionDescriptor.extensionReceiverParameter?.copy(this),
                functionDescriptor.dispatchReceiverParameter,
                functionDescriptor.typeParameters,
                paramsToAdd.mapIndexed { index, parameter ->
                    ValueParameterDescriptorImpl(
                        this, null, index, Annotations.EMPTY,
                        Name.identifier(parameter.name),
                        parameter.ktType, declaresDefaultValue = false,
                        isCrossinline = false, isNoinline = false, varargElementType = null, source = SourceElement.NO_SOURCE
                    )
                },
                functionDescriptor.returnType,
                functionDescriptor.modality,
                functionDescriptor.visibility
            )
        }


        val renderer = IdeDescriptorRenderers.SOURCE_CODE.withOptions {
            defaultParameterValueRenderer = null
        }

        val newFunction = KtPsiFactory(project).createFunction(renderer.render(newFunctionDescriptor)).apply {
            valueParameters.forEach { param ->
                param.annotationEntries.forEach { a ->
                    a.typeReference?.run {
                        val fqName = FqName(this.text)
                        if (fqName in (NULLABLE_ANNOTATIONS + NOT_NULL_ANNOTATIONS)) a.delete()
                    }
                }
            }
        }

        return newFunction.valueParameterList!!
    }

    companion object {
        fun create(ktNamedFunction: KtNamedFunction, request: ChangeParametersRequest): ChangeMethodParameters? =
            ChangeMethodParameters(ktNamedFunction, request)
    }

}