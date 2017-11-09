/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createTypeParameter

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageViewTypeLocation
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.addTypeParameter
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.InsertExplicitTypeArgumentsIntention
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.kotlin.idea.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
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
        val prefix = "type parameter".let { if (data.typeParameters.size > 1) StringUtil.pluralize(it) else it }
        val typeParametersText = if (presentTypeParameterNames) data.typeParameters.joinToString(prefix = " ") { "'${it.name}'" } else ""
        val containerText = ElementDescriptionUtil.getElementDescription(data.declaration, UsageViewTypeLocation.INSTANCE) +
                            " '${data.declaration.name}'"
        return "Create $prefix$typeParametersText in $containerText"
    }

    override fun startInWriteAction() = false

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        doInvoke()
    }

    fun doInvoke(): List<KtTypeParameter> {
        val declaration = data.declaration
        val project = declaration.project
        val usages = project.runSynchronouslyWithProgress("Searching ${declaration.name}...", true) {
            runReadAction {
                val expectedTypeArgumentCount = declaration.typeParameters.size + data.typeParameters.size
                ReferencesSearch
                        .search(declaration)
                        .mapNotNull {
                            it.element.getParentOfTypeAndBranch<KtUserType> { referenceExpression } ?:
                            it.element.getParentOfTypeAndBranch<KtCallElement> { calleeExpression }
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
                }
                else null
                val upperBound = upperBoundText?.let { psiFactory.createType(it) }
                val newTypeParameterText = if (upperBound != null) "${typeParameter.name} : ${upperBound.text}" else typeParameter.name
                val newTypeParameter = declaration.addTypeParameter(psiFactory.createTypeParameter(newTypeParameterText))!!
                elementsToShorten += newTypeParameter

                val anonymizedTypeParameter = createFakeTypeParameterDescriptor(typeParameter.fakeTypeParameter.containingDeclaration, "_")
                val anonymizedUpperBoundText = upperBoundType?.let {
                    TypeSubstitutor
                            .create(mapOf(typeParameter.fakeTypeParameter.typeConstructor to TypeProjectionImpl(anonymizedTypeParameter.defaultType)))
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
                            elementsToShorten += if (typeArgumentList != null) {
                                typeArgumentList.addArgument(anonymizedUpperBoundAsTypeArg)
                            }
                            else {
                                it.addAfter(
                                        psiFactory.createTypeArguments("<${anonymizedUpperBoundAsTypeArg.text}>"),
                                        it.referenceExpression!!
                                ) as KtTypeArgumentList
                            }
                        }
                        is KtCallElement -> {
                            if (it.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS).diagnostics.forElement(it.calleeExpression!!).any {
                                it.factory in Errors.TYPE_INFERENCE_ERRORS
                            }) {
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
                    }
                    else {
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