/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.quickfix.crossLanguage

import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.lang.jvm.actions.AnnotationRequest
import com.intellij.lang.jvm.actions.ChangeParametersRequest
import com.intellij.lang.jvm.actions.ExpectedParameter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JvmPsiConversionHelper
import org.jetbrains.kotlin.asJava.elements.KtLightElement
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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
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

    private sealed class ParameterModification {
        data class Keep(val ktParameter: KtParameter) : ParameterModification()
        data class Remove(val ktParameter: KtParameter) : ParameterModification()
        data class Add(
            val name: String,
            val ktType: KotlinType,
            val expectedAnnotations: Collection<AnnotationRequest>,
            val beforeAnchor: KtParameter?
        ) : ParameterModification()
    }

    private tailrec fun getParametersModifications(
        target: KtNamedFunction,
        currentParameters: List<KtParameter>,
        expectedParameters: List<ExpectedParameter>,
        index: Int = 0,
        collected: List<ParameterModification> = ArrayList(expectedParameters.size)
    ): List<ParameterModification> {

        val expectedHead = expectedParameters.firstOrNull() ?: return collected + currentParameters.map { ParameterModification.Remove(it) }

        if (expectedHead is ChangeParametersRequest.ExistingParameterWrapper) {
            val expectedExistingParameter = expectedHead.existingKtParameter
            if (expectedExistingParameter == null) {
                LOG.error("can't find the kotlinOrigin for parameter ${expectedHead.existingParameter} at index $index")
                return collected
            }

            val existingInTail = currentParameters.indexOf(expectedExistingParameter)
            if (existingInTail == -1) {
                throw IllegalArgumentException("can't find existing for parameter ${expectedHead.existingParameter} at index $index")
            }

            return getParametersModifications(
                target,
                currentParameters.subList(existingInTail + 1, currentParameters.size),
                expectedParameters.subList(1, expectedParameters.size),
                index,
                collected
                        + currentParameters.subList(0, existingInTail).map { ParameterModification.Remove(it) }
                        + ParameterModification.Keep(expectedExistingParameter)
            )
        }

        val helper = JvmPsiConversionHelper.getInstance(target.project)

        val theType = expectedHead.expectedTypes.firstOrNull()?.theType ?: return emptyList()
        val kotlinType = helper.convertType(theType).resolveToKotlinType(target.getResolutionFacade()) ?: return emptyList()

        return getParametersModifications(
            target,
            currentParameters,
            expectedParameters.subList(1, expectedParameters.size),
            index + 1,
            collected + ParameterModification.Add(
                expectedHead.semanticNames.firstOrNull() ?: "param$index",
                kotlinType,
                expectedHead.expectedAnnotations,
                currentParameters.firstOrNull { anchor ->
                    expectedParameters.any {
                        it is ChangeParametersRequest.ExistingParameterWrapper && it.existingKtParameter == anchor
                    }
                })
        )

    }

    private val ChangeParametersRequest.ExistingParameterWrapper.existingKtParameter
        get() = (existingParameter as? KtLightElement<*, *>)?.kotlinOrigin as? KtParameter


    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        if (!request.isValid) return

        val target = element ?: return
        val functionDescriptor = target.resolveToDescriptorIfAny(BodyResolveMode.FULL) ?: return

        val parameterActions = getParametersModifications(target, target.valueParameters, request.expectedParameters)

        val parametersGenerated = parameterActions.filterIsInstance<ParameterModification.Add>().let {
            it zip generateParameterList(project, functionDescriptor, it).parameters
        }.toMap()

        for (action in parameterActions) {
            when (action) {
                is ParameterModification.Add -> {
                    val parameter = parametersGenerated.getValue(action)
                    for (expectedAnnotation in action.expectedAnnotations) {
                        addAnnotationEntry(parameter, expectedAnnotation, null)
                    }
                    val anchor = action.beforeAnchor
                    if (anchor != null) {
                        target.valueParameterList!!.addParameterBefore(parameter, anchor)
                    } else {
                        target.valueParameterList!!.addParameter(parameter)
                    }
                }

                is ParameterModification.Keep -> {
                    // Do nothing
                }

                is ParameterModification.Remove -> {
                    target.valueParameterList!!.removeParameter(action.ktParameter)
                }
            }
        }

        ShortenReferences.DEFAULT.process(target.valueParameterList!!)
    }

    private fun generateParameterList(
        project: Project,
        functionDescriptor: FunctionDescriptor,
        paramsToAdd: List<ParameterModification.Add>
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
                        parameter.ktType, false,
                        false, false, null, SourceElement.NO_SOURCE
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

private val LOG = Logger.getInstance(ChangeMethodParameters::class.java)