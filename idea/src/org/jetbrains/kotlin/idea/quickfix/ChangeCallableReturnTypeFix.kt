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

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors.COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.isUnit
import java.util.*

abstract class ChangeCallableReturnTypeFix(
        element: KtCallableDeclaration,
        type: KotlinType
) : KotlinQuickFixAction<KtCallableDeclaration>(element) {

    private val changeFunctionLiteralReturnTypeFix: ChangeFunctionLiteralReturnTypeFix?

    private val typeContainsError = ErrorUtils.containsErrorType(type)
    private val typePresentation = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)
    private val typeSourceCode = IdeDescriptorRenderers.SOURCE_CODE.renderType(type)
    private val isUnitType = type.isUnit()

    init {
        changeFunctionLiteralReturnTypeFix = if (element is KtFunctionLiteral) {
            val functionLiteralExpression = PsiTreeUtil.getParentOfType(element, KtLambdaExpression::class.java) ?: error("FunctionLiteral outside any FunctionLiteralExpression: " + element.getElementTextWithContext())
            ChangeFunctionLiteralReturnTypeFix(functionLiteralExpression, type)
        }
        else {
            null
        }
    }

    open fun functionPresentation(): String? {
        val element = element!!
        val name = element.name
        if (name != null) {
            val container = element.unsafeResolveToDescriptor().containingDeclaration as? ClassDescriptor
            val containerName = container?.name?.takeUnless { it.isSpecial }?.asString()
            val fullName = if (containerName != null) "'$containerName.$name'" else "'$name'"
            if (element is KtParameter) {
                return "property $fullName"
            }
            return "function $fullName"
        }
        else {
            return null
        }
    }

    class OnType(element: KtFunction, type: KotlinType) : ChangeCallableReturnTypeFix(element, type), HighPriorityAction {
        override fun functionPresentation() = null
    }

    class ForEnclosing(element: KtFunction, type: KotlinType) : ChangeCallableReturnTypeFix(element, type), HighPriorityAction {
        override fun functionPresentation(): String? {
            val presentation = super.functionPresentation() ?: return "enclosing function"
            return "enclosing $presentation"
        }
    }

    class ForCalled(element: KtCallableDeclaration, type: KotlinType) : ChangeCallableReturnTypeFix(element, type) {
        override fun functionPresentation(): String? {
            val presentation = super.functionPresentation() ?: return "called function"
            return if (element is KtParameter) "accessed $presentation" else "called $presentation"
        }
    }

    class ForOverridden(element: KtFunction, type: KotlinType) : ChangeCallableReturnTypeFix(element, type) {
        override fun functionPresentation(): String? {
            val presentation = super.functionPresentation() ?: return null
            return "base $presentation"
        }
    }

    override fun getText(): String {
        val element = element ?: return ""

        if (changeFunctionLiteralReturnTypeFix != null) {
            return changeFunctionLiteralReturnTypeFix.text
        }

        val functionPresentation = functionPresentation()

        if (isUnitType && element is KtFunction && element.hasBlockBody()) {
            return if (functionPresentation == null)
                "Remove explicitly specified return type"
            else
                "Remove explicitly specified return type of $functionPresentation"
        }

        val typeName = if (element is KtFunction) "return type" else "type"

        return if (functionPresentation == null)
            "Change $typeName to '$typePresentation'"
        else
            "Change $typeName of $functionPresentation to '$typePresentation'"
    }

    override fun getFamilyName() = KotlinBundle.message("change.type.family")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        return super.isAvailable(project, editor, file) &&
               !typeContainsError &&
               element !is KtConstructor<*>
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return

        if (changeFunctionLiteralReturnTypeFix != null) {
            changeFunctionLiteralReturnTypeFix.invoke(project, editor!!, file)
        }
        else {
            if (!(isUnitType && element is KtFunction && element.hasBlockBody())) {
                var newTypeRef = KtPsiFactory(project).createType(typeSourceCode)
                newTypeRef = element.setTypeReference(newTypeRef)!!
                ShortenReferences.DEFAULT.process(newTypeRef)
            }
            else {
                element.typeReference = null
            }
        }
    }

    object ComponentFunctionReturnTypeMismatchFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val entry = getDestructuringDeclarationEntryThatTypeMismatchComponentFunction(diagnostic)
            val context = entry.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = context.get(BindingContext.COMPONENT_RESOLVED_CALL, entry) ?: return null
            val componentFunction =
                    DescriptorToSourceUtils.descriptorToDeclaration(resolvedCall.candidateDescriptor) as? KtCallableDeclaration
                    ?: return null
            val expectedType = context[BindingContext.TYPE, entry.typeReference!!] ?: return null
            return ChangeCallableReturnTypeFix.ForCalled(componentFunction, expectedType)
        }
    }

    object HasNextFunctionTypeMismatchFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val expression = QuickFixUtil.getParentElementOfType(diagnostic, KtExpression::class.java)
                             ?: error("HAS_NEXT_FUNCTION_TYPE_MISMATCH reported on element that is not within any expression")
            val context = expression.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = context[BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, expression] ?: return null
            val hasNextDescriptor = resolvedCall.candidateDescriptor
            val hasNextFunction = DescriptorToSourceUtils.descriptorToDeclaration(hasNextDescriptor) as KtFunction? ?: return null
            return ChangeCallableReturnTypeFix.ForCalled(hasNextFunction, hasNextDescriptor.builtIns.booleanType)
        }
    }

    object CompareToTypeMismatchFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val expression = QuickFixUtil.getParentElementOfType(diagnostic, KtBinaryExpression::class.java) ?: error("COMPARE_TO_TYPE_MISMATCH reported on element that is not within any expression")
            val context = expression.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = expression.getResolvedCall(context) ?: return null
            val compareToDescriptor = resolvedCall.candidateDescriptor
            val compareTo = DescriptorToSourceUtils.descriptorToDeclaration(compareToDescriptor) as? KtFunction ?: return null
            return ChangeCallableReturnTypeFix.ForCalled(compareTo, compareToDescriptor.builtIns.intType)
        }
    }

    object ReturnTypeMismatchOnOverrideFactory : KotlinIntentionActionsFactory() {
        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val function = QuickFixUtil.getParentElementOfType(diagnostic, KtFunction::class.java) ?: return emptyList()

            val actions = LinkedList<IntentionAction>()

            val descriptor = function.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? FunctionDescriptor ?: return emptyList()

            val matchingReturnType = QuickFixUtil.findLowerBoundOfOverriddenCallablesReturnTypes(descriptor)
            if (matchingReturnType != null) {
                actions.add(ChangeCallableReturnTypeFix.OnType(function, matchingReturnType))
            }

            val functionType = descriptor.returnType ?: return actions

            val overriddenMismatchingFunctions = LinkedList<FunctionDescriptor>()
            for (overriddenFunction in descriptor.overriddenDescriptors) {
                val overriddenFunctionType = overriddenFunction.returnType ?: continue
                if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(functionType, overriddenFunctionType)) {
                    overriddenMismatchingFunctions.add(overriddenFunction)
                }
            }

            if (overriddenMismatchingFunctions.size == 1) {
                val overriddenFunction = DescriptorToSourceUtils.descriptorToDeclaration(overriddenMismatchingFunctions[0])
                if (overriddenFunction is KtFunction) {
                    actions.add(ChangeCallableReturnTypeFix.ForOverridden(overriddenFunction, functionType))
                }
            }

            return actions
        }
    }

    object ChangingReturnTypeToUnitFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val function = QuickFixUtil.getParentElementOfType(diagnostic, KtFunction::class.java) ?: return null
            return ChangeCallableReturnTypeFix.ForEnclosing(function, function.builtIns.unitType)
        }
    }

    object ChangingReturnTypeToNothingFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val function = QuickFixUtil.getParentElementOfType(diagnostic, KtFunction::class.java) ?: return null
            return ChangeCallableReturnTypeFix.ForEnclosing(function, function.builtIns.nothingType)
        }
    }

    companion object {
        fun getDestructuringDeclarationEntryThatTypeMismatchComponentFunction(diagnostic: Diagnostic): KtDestructuringDeclarationEntry {
            val componentName = COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.cast(diagnostic).a
            val componentIndex = DataClassDescriptorResolver.getComponentIndex(componentName.asString())
            val multiDeclaration = QuickFixUtil.getParentElementOfType(diagnostic, KtDestructuringDeclaration::class.java) ?: error("COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH reported on expression that is not within any multi declaration")
            return multiDeclaration.entries[componentIndex - 1]
        }
    }
}
