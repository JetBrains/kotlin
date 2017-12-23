/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintsUtil
import org.jetbrains.kotlin.resolve.calls.inference.InferenceErrorData
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.resolve.calls.inference.filterConstraintsOut
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance

class AddGenericUpperBoundFix(
        typeParameter: KtTypeParameter,
        upperBound: KotlinType
) : KotlinQuickFixAction<KtTypeParameter>(typeParameter) {
    private val renderedUpperBound: String = IdeDescriptorRenderers.SOURCE_CODE.renderType(upperBound)

    override fun getText() = element?.let { "Add '$renderedUpperBound' as upper bound for ${it.name}" } ?: ""
    override fun getFamilyName() = "Add generic upper bound"

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        val element = element ?: return false
        // TODO: replacing existing upper bounds
        return (element.name != null && element.extendsBound == null)
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        assert(element.extendsBound == null) { "Don't know what to do with existing bounds" }

        val typeReference = KtPsiFactory(project).createType(renderedUpperBound)
        val insertedTypeReference = element.setExtendsBound(typeReference)!!

        ShortenReferences.DEFAULT.process(insertedTypeReference)
    }

    companion object Factory : KotlinIntentionActionsFactory() {
        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            return when (diagnostic.factory) {
                Errors.UPPER_BOUND_VIOLATED -> {
                    val upperBoundViolated = Errors.UPPER_BOUND_VIOLATED.cast(diagnostic)
                    listOfNotNull(createAction(upperBoundViolated.b, upperBoundViolated.a))
                }
                Errors.TYPE_INFERENCE_UPPER_BOUND_VIOLATED -> {
                    val inferenceData = Errors.TYPE_INFERENCE_UPPER_BOUND_VIOLATED.cast(diagnostic).a
                    createActionsByInferenceData(inferenceData)
                }
                else -> emptyList()
            }
        }

        private fun createActionsByInferenceData(inferenceData: InferenceErrorData): List<IntentionAction> {
            val successfulConstraintSystem = inferenceData.constraintSystem.filterConstraintsOut(ConstraintPositionKind.TYPE_BOUND_POSITION)

            if (!successfulConstraintSystem.status.isSuccessful()) return emptyList()

            val resultingSubstitutor = successfulConstraintSystem.resultingSubstitutor

            return inferenceData.descriptor.typeParameters.mapNotNull factory@{
                typeParameterDescriptor ->

                if (ConstraintsUtil.checkUpperBoundIsSatisfied(
                        successfulConstraintSystem, typeParameterDescriptor, inferenceData.call,
                        /* substituteOtherTypeParametersInBound */ true
                )) return@factory null

                val upperBound = typeParameterDescriptor.upperBounds.singleOrNull() ?: return@factory null
                val argument = resultingSubstitutor.substitute(typeParameterDescriptor.defaultType, Variance.INVARIANT)
                               ?: return@factory null

                createAction(argument, upperBound)
            }
        }

        private fun createAction(argument: KotlinType, upperBound: KotlinType): IntentionAction? {
            if (!upperBound.constructor.isDenotable) return null

            val typeParameterDescriptor = (argument.constructor.declarationDescriptor as? TypeParameterDescriptor) ?: return null
            val typeParameterDeclaration =
                    (DescriptorToSourceUtils.getSourceFromDescriptor(typeParameterDescriptor) as? KtTypeParameter) ?: return null

            return AddGenericUpperBoundFix(typeParameterDeclaration, upperBound)
        }
    }
}
