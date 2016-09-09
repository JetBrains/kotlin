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
import org.jetbrains.kotlin.idea.refactoring.runSynchronouslyWithProgress
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
        originalElement: KtTypeElement,
        private val data: CreateTypeParameterData
) : CreateFromUsageFixBase<KtTypeElement>(originalElement) {
    override fun getText() = "Create type parameter '${data.name}' in " +
                             ElementDescriptionUtil.getElementDescription(data.declaration, UsageViewTypeLocation.INSTANCE) + " '${data.declaration.name}'"

    override fun startInWriteAction() = false

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        doInvoke()
    }

    fun doInvoke(): KtTypeParameter? {
        val declaration = data.declaration
        val project = declaration.project
        val usages = project.runSynchronouslyWithProgress("Searching ${declaration.name}...", true) {
            runReadAction {
                ReferencesSearch
                        .search(declaration)
                        .mapNotNull {
                            it.element.getParentOfTypeAndBranch<KtUserType> { referenceExpression } ?:
                            it.element.getParentOfTypeAndBranch<KtCallElement> { calleeExpression }
                        }
                        .toSet()
            }
        } ?: return null

        return runWriteAction {
            val psiFactory = KtPsiFactory(project)

            val elementsToShorten = SmartList<KtElement>()

            val upperBoundType = data.upperBoundType
            val upperBoundText = if (upperBoundType != null && !upperBoundType.isNullableAny()) {
                IdeDescriptorRenderers.SOURCE_CODE.renderType(upperBoundType)
            }
            else null
            val upperBound = upperBoundText?.let { psiFactory.createType(it) }
            val newTypeParameterText = if (upperBound != null) "${data.name} : ${upperBound.text}" else data.name
            val newTypeParameter = declaration.addTypeParameter(psiFactory.createTypeParameter(newTypeParameterText))!!
            elementsToShorten += newTypeParameter

            val anonymizedTypeParameter = createFakeTypeParameterDescriptor(data.fakeTypeParameter.containingDeclaration, "_")
            val anonymizedUpperBoundText = upperBoundType?.let {
                TypeSubstitutor
                        .create(mapOf(data.fakeTypeParameter.typeConstructor to TypeProjectionImpl(anonymizedTypeParameter.defaultType)))
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
                        if (it.analyze(BodyResolveMode.PARTIAL).diagnostics.forElement(it.calleeExpression!!).any {
                            it.factory in Errors.TYPE_INFERENCE_ERRORS
                        }) {
                            callsToExplicateArguments += it
                        }
                    }
                }
            }

            callsToExplicateArguments.forEach {
                val typeArgumentList = it.typeArgumentList
                if (typeArgumentList == null) {
                    InsertExplicitTypeArgumentsIntention.applyTo(it, shortenReferences = false)

                    val newTypeArgument = it.typeArguments.lastOrNull()
                    if (anonymizedUpperBoundText != null && newTypeArgument != null && newTypeArgument.text == "kotlin.Any") {
                        newTypeArgument.replaced(anonymizedUpperBoundAsTypeArg)
                    }

                    elementsToShorten += it.typeArgumentList
                }
                else {
                    elementsToShorten += typeArgumentList.addArgument(anonymizedUpperBoundAsTypeArg)
                }
            }

            ShortenReferences.DEFAULT.process(elementsToShorten)

            newTypeParameter
        }
    }
}