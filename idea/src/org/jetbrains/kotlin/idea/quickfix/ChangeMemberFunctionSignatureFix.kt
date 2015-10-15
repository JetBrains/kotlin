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

import com.google.common.collect.Lists
import com.google.common.collect.Queues
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.JetParameterList
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.NameShortness
import org.jetbrains.kotlin.resolve.FunctionDescriptorUtil
import org.jetbrains.kotlin.resolve.VisibilityUtil
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.util.*

/**
 * Fix that changes member function's signature to match one of super functions' signatures.
 */
class ChangeMemberFunctionSignatureFix(element: JetNamedFunction) : JetHintAction<JetNamedFunction>(element) {

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val function = diagnostic.psiElement as? JetNamedFunction ?: return null
            return ChangeMemberFunctionSignatureFix(function)
        }

        private val SIGNATURE_SOURCE_RENDERER = IdeDescriptorRenderers.SOURCE_CODE.withOptions {
            renderDefaultValues = false
        }

        private val SIGNATURE_PREVIEW_RENDERER = DescriptorRenderer.withOptions {
            typeNormalizer = IdeDescriptorRenderers.APPROXIMATE_FLEXIBLE_TYPES
            withDefinedIn = false
            modifiers = emptySet<DescriptorRendererModifier>()
            nameShortness = NameShortness.SHORT
            unitReturnType = false
            renderDefaultValues = false
        }

        /**
         * Computes all the signatures a 'functionElement' could be changed to in order to remove NOTHING_TO_OVERRIDE error.
         */
        private fun computePossibleSignatures(functionElement: JetNamedFunction): List<FunctionDescriptor> {
            if (functionElement.valueParameterList == null) {
                // we won't be able to modify its signature
                return emptyList()
            }

            val functionDescriptor = functionElement.resolveToDescriptor() as FunctionDescriptor
            val superFunctions = getPossibleSuperFunctionsDescriptors(functionDescriptor)

            val possibleSignatures: Map<String, FunctionDescriptor> = superFunctions
                    .filter { it.kind.isReal }
                    .map { changeSignatureToMatch(functionDescriptor, it) }
                    .toMapBy { SIGNATURE_PREVIEW_RENDERER.render(it) }

            return possibleSignatures.keys
                    .sorted()
                    .map { possibleSignatures[it]!! }
        }

        /**
         * Changes function's signature to match superFunction's signature. Returns new descriptor.
         */
        private fun changeSignatureToMatch(function: FunctionDescriptor, superFunction: FunctionDescriptor): FunctionDescriptor {
            val superParameters = superFunction.valueParameters
            val parameters = function.valueParameters
            val newParameters = Lists.newArrayList(superParameters)

            // Parameters in superFunction, which are matched in new function signature:
            val matched = BitSet(superParameters.size)
            // Parameters in this function, which are used in new function signature:
            val used = BitSet(superParameters.size)

            matchParameters(ParameterChooser.MatchNames, superParameters, parameters, newParameters, matched, used)
            matchParameters(ParameterChooser.MatchTypes, superParameters, parameters, newParameters, matched, used)

            val newFunction = FunctionDescriptorUtil.replaceFunctionParameters(
                    superFunction.copy(
                            function.containingDeclaration,
                            Modality.OPEN,
                            getVisibility(function, superFunction),
                            CallableMemberDescriptor.Kind.DELEGATION,
                            /* copyOverrides = */ true),
                    newParameters)
            newFunction.addOverriddenDescriptor(superFunction)
            return newFunction
        }

        /**
         * Returns new visibility for 'function' modified to override 'superFunction'.
         */
        private fun getVisibility(function: FunctionDescriptor, superFunction: FunctionDescriptor): Visibility {
            val descriptors = Queues.newArrayDeque<CallableMemberDescriptor>(listOf(superFunction, function))
            return VisibilityUtil.findMemberWithMaxVisibility(descriptors).visibility
        }

        /**
         * Match function's parameters with super function's parameters using parameterChooser.
         * Doesn't have to preserve ordering, parameter names or types.
         * @param superParameters - super function's parameters
         * *
         * @param parameters - function's parameters
         * *
         * @param newParameters - new parameters (may be modified by this function)
         * *
         * @param matched - true iff this parameter in super function is matched by some parameter in function (may be modified by this function)
         * *
         * @param used - true iff this parameter in function is used to match some parameter in super function (may be modified by this function)
         */
        private fun matchParameters(
                parameterChooser: ParameterChooser,
                superParameters: List<ValueParameterDescriptor>,
                parameters: List<ValueParameterDescriptor>,
                newParameters: MutableList<ValueParameterDescriptor>,
                matched: BitSet,
                used: BitSet
        ) {
            for (superParameter in superParameters) {
                if (!matched[superParameter.index]) {
                    for (parameter in parameters) {
                        val choice = parameterChooser.choose(parameter, superParameter)
                        if (choice != null && !used[parameter.index]) {
                            used[parameter.index] = true
                            matched[superParameter.index] = true
                            newParameters[superParameter.index] = choice
                            break
                        }
                    }
                }
            }
        }

        /**
         * Returns all open functions in superclasses which have the same name as 'functionDescriptor' (but possibly
         * different parameters/return type).
         */
        private fun getPossibleSuperFunctionsDescriptors(functionDescriptor: FunctionDescriptor): List<FunctionDescriptor> {
            val containingClass = functionDescriptor.containingDeclaration as? ClassDescriptor ?: return emptyList()

            val name = functionDescriptor.name
            return containingClass.defaultType.supertypes()
                    .flatMap { supertype -> supertype.memberScope.getFunctions(name, NoLookupLocation.FROM_IDE) }
                    .filter { it.kind.isReal && it.modality.isOverridable }
        }
    }

    private val possibleSignatures = computePossibleSignatures(element)

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        return super.isAvailable(project, editor, file) && !possibleSignatures.isEmpty()
    }

    override fun getText(): String {
        val single = possibleSignatures.singleOrNull()
        if (single != null) {
            return "Change function signature to '${SIGNATURE_PREVIEW_RENDERER.render(single)}'"
        }
        else {
            return "Change function signature..."
        }
    }

    override fun getFamilyName() = "Change function signature"

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        CommandProcessor.getInstance().runUndoTransparentAction {
            MyAction(project, editor, element, possibleSignatures).execute()
        }
    }

    /** Helper interface for matchParameters(..) method.  */
    private interface ParameterChooser {
        /**
         * Checks if 'parameter' may be used to match 'superParameter'.
         * If so, returns (possibly modified) descriptor to be used as the new parameter.
         * If not, returns null.
         */
        fun choose(parameter: ValueParameterDescriptor, superParameter: ValueParameterDescriptor): ValueParameterDescriptor?

        object MatchNames : ParameterChooser {
            override fun choose(parameter: ValueParameterDescriptor, superParameter: ValueParameterDescriptor): ValueParameterDescriptor? {
                return superParameter.check { parameter.name == superParameter.name }
            }
        }

        object MatchTypes : ParameterChooser {
            override fun choose(parameter: ValueParameterDescriptor, superParameter: ValueParameterDescriptor): ValueParameterDescriptor? {
                // TODO: support for generic functions
                if (JetTypeChecker.DEFAULT.equalTypes(parameter.type, superParameter.type)) {
                    return superParameter.copy(parameter.containingDeclaration, parameter.name)
                }
                else {
                    return null
                }
            }
        }

    }

    override fun showHint(editor: Editor): Boolean {
        return possibleSignatures.isNotEmpty() && !HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)
    }

    private class MyAction(
            private val project: Project,
            private val editor: Editor?,
            private val function: JetNamedFunction,
            private val signatures: List<FunctionDescriptor>
    ) {
        fun execute() {
            PsiDocumentManager.getInstance(project).commitAllDocuments()

            if (!function.isValid || signatures.isEmpty()) return

            if (signatures.size == 1 || editor == null || !editor.component.isShowing) {
                changeSignature(signatures.first())
            }
            else {
                chooseSignatureAndChange()
            }
        }

        private val signaturePopup: BaseListPopupStep<FunctionDescriptor>
            get() {
                return object : BaseListPopupStep<FunctionDescriptor>("Choose Signature", signatures) {
                    override fun isAutoSelectionEnabled() = false

                    override fun onChosen(selectedValue: FunctionDescriptor, finalChoice: Boolean): PopupStep<Any>? {
                        if (finalChoice) {
                            changeSignature(selectedValue)
                        }
                        return PopupStep.FINAL_CHOICE
                    }

                    override fun getIconFor(aValue: FunctionDescriptor) = PlatformIcons.FUNCTION_ICON

                    override fun getTextFor(aValue: FunctionDescriptor) = SIGNATURE_PREVIEW_RENDERER.render(aValue)
                }
            }

        private fun changeSignature(patternDescriptor: FunctionDescriptor) {
            val signatureString = SIGNATURE_SOURCE_RENDERER.render(patternDescriptor)

            PsiDocumentManager.getInstance(project).commitAllDocuments()

            project.executeWriteCommand("Change Function Signature") {
                val patternFunction = JetPsiFactory(project).createFunction(signatureString)

                val newTypeRef = function.setTypeReference(patternFunction.typeReference)
                if (newTypeRef != null) {
                    ShortenReferences.DEFAULT.process(newTypeRef)
                }

                val newParameterList = function.valueParameterList!!.replace(patternFunction.valueParameterList!!) as JetParameterList
                ShortenReferences.DEFAULT.process(newParameterList)
            }
        }

        private fun chooseSignatureAndChange() {
            JBPopupFactory.getInstance().createListPopup(signaturePopup).showInBestPositionFor(editor!!)
        }
    }
}
