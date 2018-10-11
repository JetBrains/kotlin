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

package org.jetbrains.kotlin.idea.quickfix

import com.google.common.collect.Lists
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.NOT_NULL_ANNOTATIONS
import org.jetbrains.kotlin.load.java.NULLABLE_ANNOTATIONS
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.typeRefHelpers.setReceiverTypeReference
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.descriptorUtil.setSingleOverridden
import org.jetbrains.kotlin.resolve.findMemberWithMaxVisibility
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.supertypes
import java.util.*

/**
 * Fix that changes member function's signature to match one of super functions' signatures.
 */
class ChangeMemberFunctionSignatureFix private constructor(
    element: KtNamedFunction,
    private val signatures: List<ChangeMemberFunctionSignatureFix.Signature>
) : KotlinQuickFixAction<KtNamedFunction>(element) {

    init {
        assert(signatures.isNotEmpty())
    }

    private class Signature(function: FunctionDescriptor) {
        val sourceCode = SIGNATURE_SOURCE_RENDERER.render(function)
        val preview = SIGNATURE_PREVIEW_RENDERER.render(function)

        companion object {
            private val SIGNATURE_SOURCE_RENDERER = IdeDescriptorRenderers.SOURCE_CODE.withOptions {
                defaultParameterValueRenderer = null
            }

            private val SIGNATURE_PREVIEW_RENDERER = DescriptorRenderer.withOptions {
                typeNormalizer = IdeDescriptorRenderers.APPROXIMATE_FLEXIBLE_TYPES
                withDefinedIn = false
                modifiers = emptySet()
                classifierNamePolicy = ClassifierNamePolicy.SHORT
                unitReturnType = false
                defaultParameterValueRenderer = null
            }
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val function = diagnostic.psiElement as? KtNamedFunction ?: return null
            val signatures = computePossibleSignatures(function)
            if (signatures.isEmpty()) return null
            return ChangeMemberFunctionSignatureFix(function, signatures)
        }

        /**
         * Computes all the signatures a 'functionElement' could be changed to in order to remove NOTHING_TO_OVERRIDE error.
         */
        private fun computePossibleSignatures(functionElement: KtNamedFunction): List<Signature> {
            if (functionElement.valueParameterList == null) {
                // we won't be able to modify its signature
                return emptyList()
            }

            val functionDescriptor = functionElement.resolveToDescriptorIfAny(BodyResolveMode.FULL) ?: return emptyList()
            val superFunctions = getPossibleSuperFunctionsDescriptors(functionDescriptor)

            return superFunctions
                .asSequence()
                .filter { it.kind.isReal }
                .map { signatureToMatch(functionDescriptor, it) }
                .distinctBy { it.sourceCode }
                .sortedBy { it.preview }
                .toList()
        }

        /**
         * Changes function's signature to match superFunction's signature. Returns new descriptor.
         */
        private fun signatureToMatch(function: FunctionDescriptor, superFunction: FunctionDescriptor): Signature {
            val superParameters = superFunction.valueParameters
            val parameters = function.valueParameters
            val newParameters = Lists.newArrayList(superParameters)

            // Parameters in superFunction, which are matched in new function signature:
            val matched = BitSet(superParameters.size)
            // Parameters in this function, which are used in new function signature:
            val used = BitSet(superParameters.size)

            matchParameters(ParameterChooser.MatchNames, superParameters, parameters, newParameters, matched, used)
            matchParameters(ParameterChooser.MatchTypes, superParameters, parameters, newParameters, matched, used)

            val newFunction = replaceFunctionParameters(
                superFunction.copy(
                    function.containingDeclaration,
                    Modality.OPEN,
                    findMemberWithMaxVisibility(listOf(superFunction, function)).visibility,
                    CallableMemberDescriptor.Kind.DELEGATION,
                    /* copyOverrides = */ true
                ),
                newParameters
            )
            newFunction.setSingleOverridden(superFunction)

            return Signature(newFunction)
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
                .flatMap { supertype -> supertype.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_IDE) }
                .filter { it.kind.isReal && it.isOverridable }
        }

        /**
         * Returns function's copy with new parameter list.
         * Note that parameters may belong to other methods or have incorrect "index" property -- it will be fixed by this function.
         */
        private fun replaceFunctionParameters(
            function: FunctionDescriptor,
            newParameters: List<ValueParameterDescriptor>
        ): FunctionDescriptor {
            val descriptor = SimpleFunctionDescriptorImpl.create(
                function.containingDeclaration,
                function.annotations,
                function.name,
                function.kind,
                SourceElement.NO_SOURCE
            )

            val parameters = newParameters.asSequence().withIndex().map { (index, parameter) ->
                ValueParameterDescriptorImpl(
                    descriptor, null, index,
                    parameter.annotations, parameter.name, parameter.returnType!!, parameter.declaresDefaultValue(),
                    parameter.isCrossinline, parameter.isNoinline, parameter.varargElementType, SourceElement.NO_SOURCE
                )
            }.toList()

            return descriptor.apply {
                initialize(
                    function.extensionReceiverParameter?.copy(this), function.dispatchReceiverParameter,
                    function.typeParameters, parameters, function.returnType, function.modality, function.visibility
                )
                isOperator = function.isOperator
                isInfix = function.isInfix
                isExternal = function.isExternal
                isInline = function.isInline
                isTailrec = function.isTailrec
            }
        }
    }

    override fun getText(): String {
        val single = signatures.singleOrNull()
        return if (single != null)
            "Change function signature to '${single.preview}'"
        else
            "Change function signature..."
    }

    override fun getFamilyName() = "Change function signature"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        CommandProcessor.getInstance().runUndoTransparentAction {
            MyAction(project, editor, element, signatures).execute()
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
                return superParameter.takeIf { parameter.name == superParameter.name }
            }
        }

        object MatchTypes : ParameterChooser {
            override fun choose(parameter: ValueParameterDescriptor, superParameter: ValueParameterDescriptor): ValueParameterDescriptor? {
                // TODO: support for generic functions
                return if (KotlinTypeChecker.DEFAULT.equalTypes(parameter.type, superParameter.type)) {
                    superParameter.copy(parameter.containingDeclaration, parameter.name, parameter.index)
                } else {
                    null
                }
            }
        }

    }

    private class MyAction(
        private val project: Project,
        private val editor: Editor?,
        private val function: KtNamedFunction,
        private val signatures: List<Signature>
    ) {
        fun execute() {
            PsiDocumentManager.getInstance(project).commitAllDocuments()

            if (!function.isValid || signatures.isEmpty()) return

            if (signatures.size == 1 || editor == null || !editor.component.isShowing) {
                changeSignature(signatures.first())
            } else {
                chooseSignatureAndChange()
            }
        }

        private val signaturePopup: BaseListPopupStep<Signature>
            get() {
                return object : BaseListPopupStep<Signature>("Choose Signature", signatures) {
                    override fun isAutoSelectionEnabled() = false

                    override fun onChosen(selectedValue: Signature, finalChoice: Boolean): PopupStep<Any>? {
                        if (finalChoice) {
                            changeSignature(selectedValue)
                        }
                        return PopupStep.FINAL_CHOICE
                    }

                    override fun getIconFor(aValue: Signature) = PlatformIcons.FUNCTION_ICON

                    override fun getTextFor(aValue: Signature) = aValue.preview
                }
            }

        private fun changeSignature(signature: Signature) {
            PsiDocumentManager.getInstance(project).commitAllDocuments()

            project.executeWriteCommand("Change Function Signature") {
                val patternFunction = KtPsiFactory(project).createFunction(signature.sourceCode)

                val newTypeRef = function.setTypeReference(patternFunction.typeReference)
                if (newTypeRef != null) {
                    ShortenReferences.DEFAULT.process(newTypeRef)
                }

                patternFunction.valueParameters.forEach { param ->
                    param.annotationEntries.forEach { a ->
                        a.typeReference?.run {
                            val fqName = FqName(this.text)
                            if (fqName in (NULLABLE_ANNOTATIONS + NOT_NULL_ANNOTATIONS)) a.delete()
                        }
                    }
                }

                val newParameterList = function.valueParameterList!!.replace(patternFunction.valueParameterList!!) as KtParameterList
                if (patternFunction.receiverTypeReference == null && function.receiverTypeReference != null) {
                    function.setReceiverTypeReference(null)
                }
                ShortenReferences.DEFAULT.process(newParameterList)
            }
        }

        private fun chooseSignatureAndChange() {
            JBPopupFactory.getInstance().createListPopup(signaturePopup).showInBestPositionFor(editor!!)
        }
    }
}
