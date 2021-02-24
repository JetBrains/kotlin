/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createTypeParameter

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageViewTypeLocation
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.addTypeParameter
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.core.util.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.intentions.InsertExplicitTypeArgumentsIntention
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.isNullableAny
import org.jetbrains.kotlin.utils.SmartList

class CreateTypeParameterFromUsageFix(
    originalElement: KtElement,
    private val data: CreateTypeParameterData,
    private val presentTypeParameterNames: Boolean
) : CreateFromUsageFixBase<KtElement>(originalElement) {
    override fun getText(): String {
        val prefix = KotlinBundle.message("text.type.parameter", data.typeParameters.size)
        val typeParametersText = if (presentTypeParameterNames) data.typeParameters.joinToString(prefix = " ") { "'${it.name}'" } else ""
        val containerText = ElementDescriptionUtil.getElementDescription(data.declaration, UsageViewTypeLocation.INSTANCE) +
                " '${data.declaration.name}'"
        return KotlinBundle.message("create.0.in.1", prefix + typeParametersText, containerText)
    }

    override fun startInWriteAction() = false

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        doInvoke()
    }

    fun doInvoke(): List<KtTypeParameter> {
        val declaration = data.declaration
        if (!declaration.isWritable) return emptyList()
        val project = declaration.project
        val usages = project.runSynchronouslyWithProgress(KotlinBundle.message("searching.0", declaration.name.toString()), true) {
            runReadAction {
                val expectedTypeArgumentCount = declaration.typeParameters.size + data.typeParameters.size
                ReferencesSearch
                    .search(declaration)
                    .mapNotNull {
                        it.element.getParentOfTypeAndBranch<KtUserType> { referenceExpression }
                            ?: it.element.getParentOfTypeAndBranch<KtCallElement> { calleeExpression }
                    }
                    .filter {
                        val arguments = when (it) {
                            is KtUserType -> it.typeArguments
                            is KtCallElement -> it.typeArguments
                            else -> return@filter false
                        }
                        arguments.size != expectedTypeArgumentCount
                    }
                    .toSet()
            }
        } ?: return emptyList()

        return runWriteAction {
            val psiFactory = KtPsiFactory(project)

            val elementsToShorten = SmartList<KtElement>()

            val newTypeParameters = data.typeParameters.map { typeParameter ->
                val upperBoundType = typeParameter.upperBoundType
                val upperBoundText = if (upperBoundType != null && !upperBoundType.isNullableAny()) {
                    IdeDescriptorRenderers.SOURCE_CODE.renderType(upperBoundType)
                } else null
                val upperBound = upperBoundText?.let { psiFactory.createType(it) }
                val typeParameterName = typeParameter.name.quoteIfNeeded()
                val newTypeParameterText = if (upperBound != null) "$typeParameterName : ${upperBound.text}" else typeParameterName
                val newTypeParameter = declaration.addTypeParameter(psiFactory.createTypeParameter(newTypeParameterText))
                    ?: error("Couldn't create type parameter from '$newTypeParameterText' for '$declaration'")
                elementsToShorten += newTypeParameter

                val anonymizedTypeParameter = createFakeTypeParameterDescriptor(
                    typeParameter.fakeTypeParameter.containingDeclaration, "_", typeParameter.fakeTypeParameter.storageManager
                )
                val anonymizedUpperBoundText = upperBoundType?.let {
                    TypeSubstitutor.create(
                            mapOf(
                                typeParameter.fakeTypeParameter.typeConstructor to TypeProjectionImpl(
                                    anonymizedTypeParameter.defaultType
                                )
                            )
                        )
                        .substitute(upperBoundType, Variance.INVARIANT)
                }?.let {
                    IdeDescriptorRenderers.SOURCE_CODE.renderType(it)
                }

                val anonymizedUpperBoundAsTypeArg = psiFactory.createTypeArgument(anonymizedUpperBoundText ?: "kotlin.Any?")

                val callsToExplicateArguments = SmartList<KtCallElement>()
                usages.forEach {
                    when (it) {
                        is KtUserType -> {
                            val typeArgumentList = it.typeArgumentList
                            elementsToShorten += typeArgumentList?.addArgument(anonymizedUpperBoundAsTypeArg) ?: it.addAfter(
                                psiFactory.createTypeArguments("<${anonymizedUpperBoundAsTypeArg.text}>"),
                                it.referenceExpression!!
                            ) as KtTypeArgumentList
                        }
                        is KtCallElement -> {
                            if (it.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS).diagnostics.forElement(it.calleeExpression!!)
                                    .any { diagnostic -> diagnostic.factory in Errors.TYPE_INFERENCE_ERRORS }
                            ) {
                                callsToExplicateArguments += it
                            }
                        }
                    }
                }

                callsToExplicateArguments.forEach {
                    val typeArgumentList = it.typeArgumentList
                    elementsToShorten += if (typeArgumentList == null) {
                        InsertExplicitTypeArgumentsIntention.applyTo(it, shortenReferences = false)

                        val newTypeArgument = it.typeArguments.lastOrNull()
                        if (anonymizedUpperBoundText != null && newTypeArgument != null && newTypeArgument.text == "kotlin.Any") {
                            newTypeArgument.replaced(anonymizedUpperBoundAsTypeArg)
                        }

                        it.typeArgumentList
                    } else {
                        typeArgumentList.addArgument(anonymizedUpperBoundAsTypeArg)
                    }
                }

                newTypeParameter
            }

            ShortenReferences.DEFAULT.process(elementsToShorten)

            newTypeParameters
        }
    }
}