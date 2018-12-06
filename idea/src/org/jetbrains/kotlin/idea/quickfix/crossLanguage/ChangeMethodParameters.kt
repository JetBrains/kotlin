/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.quickfix.crossLanguage

import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.load.java.NOT_NULL_ANNOTATIONS
import org.jetbrains.kotlin.load.java.NULLABLE_ANNOTATIONS
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

internal class ChangeMethodParameters(
    target: KtNamedFunction,
    private val request: List<Pair<Name, KotlinType>>,
    private val isValid: () -> Boolean
) : KotlinQuickFixAction<KtNamedFunction>(target) {

    override fun getText(): String {
        val parametersString = request.joinToString(", ", "(", ")") { (name, type) ->
            "${name.asString()}: ${IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(type)}"
        }

        val shortenParameterString = StringUtil.shortenTextWithEllipsis(parametersString, 30, 5)
        return QuickFixBundle.message("change.method.parameters.text", shortenParameterString)
    }

    override fun getFamilyName(): String = QuickFixBundle.message("change.method.parameters.family")

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean = element != null && isValid()

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val target = element ?: return
        val functionDescriptor = target.resolveToDescriptorIfAny(BodyResolveMode.FULL) ?: return

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
                request.withIndex().map { (index, parameter) ->
                    ValueParameterDescriptorImpl(
                        this, null, index, Annotations.EMPTY,
                        Name.identifier(parameter.first.toString()),
                        parameter.second, false,
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

        val newParameterList = target.valueParameterList!!.replace(newFunction.valueParameterList!!) as KtParameterList
        ShortenReferences.DEFAULT.process(newParameterList)
    }


}