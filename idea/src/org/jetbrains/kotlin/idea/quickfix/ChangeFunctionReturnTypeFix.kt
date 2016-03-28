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
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors.COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.dataClassUtils.getComponentIndex
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import java.util.*

class ChangeFunctionReturnTypeFix(element: KtFunction, private val type: KotlinType) : KotlinQuickFixAction<KtFunction>(element) {
    private val changeFunctionLiteralReturnTypeFix: ChangeFunctionLiteralReturnTypeFix?

    init {
        if (element is KtFunctionLiteral) {
            val functionLiteralExpression = PsiTreeUtil.getParentOfType(element, KtLambdaExpression::class.java) ?: error("FunctionLiteral outside any FunctionLiteralExpression: " + element.getElementTextWithContext())
            changeFunctionLiteralReturnTypeFix = ChangeFunctionLiteralReturnTypeFix(functionLiteralExpression, type)
        }
        else {
            changeFunctionLiteralReturnTypeFix = null
        }
    }

    override fun getText(): String {
        if (changeFunctionLiteralReturnTypeFix != null) {
            return changeFunctionLiteralReturnTypeFix.text
        }

        var functionName = element.name
        val fqName = element.fqName
        if (fqName != null) functionName = fqName.asString()

        if (KotlinBuiltIns.isUnit(type) && element.hasBlockBody()) {
            return if (functionName == null)
                KotlinBundle.message("remove.no.name.function.return.type")
            else
                KotlinBundle.message("remove.function.return.type", functionName)
        }
        val renderedType = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)
        return if (functionName == null)
            KotlinBundle.message("change.no.name.function.return.type", renderedType)
        else
            KotlinBundle.message("change.function.return.type", functionName, renderedType)
    }

    override fun getFamilyName(): String {
        return KotlinBundle.message("change.type.family")
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        return super.isAvailable(project, editor, file) &&
               !ErrorUtils.containsErrorType(type) &&
               element !is KtConstructor<*>
    }

    @Throws(IncorrectOperationException::class)
    public override operator fun invoke(project: Project, editor: Editor?, file: KtFile) {
        if (changeFunctionLiteralReturnTypeFix != null) {
            changeFunctionLiteralReturnTypeFix.invoke(project, editor!!, file)
        }
        else {
            if (!(KotlinBuiltIns.isUnit(type) && element.hasBlockBody())) {
                var newTypeRef: KtTypeReference? = KtPsiFactory(project).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))
                newTypeRef = element.setTypeReference(newTypeRef)
                assert(newTypeRef != null)
                ShortenReferences.DEFAULT.process(newTypeRef!!)
            }
            else {
                element.typeReference = null
            }
        }
    }

    companion object {

        fun getDestructuringDeclarationEntryThatTypeMismatchComponentFunction(diagnostic: Diagnostic): KtDestructuringDeclarationEntry {
            val componentName = COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.cast(diagnostic).a
            val componentIndex = getComponentIndex(componentName)
            val multiDeclaration = QuickFixUtil.getParentElementOfType(diagnostic, KtDestructuringDeclaration::class.java) ?: error("COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH reported on expression that is not within any multi declaration")
            return multiDeclaration.entries[componentIndex - 1]
        }

        fun createFactoryForComponentFunctionReturnTypeMismatch(): KotlinSingleIntentionActionFactory {
            return object : KotlinSingleIntentionActionFactory() {
                public override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                    val entry = getDestructuringDeclarationEntryThatTypeMismatchComponentFunction(diagnostic)
                    val context = entry.analyze()
                    val resolvedCall = context.get(BindingContext.COMPONENT_RESOLVED_CALL, entry) ?: return null
                    val componentFunction = DescriptorToSourceUtils.descriptorToDeclaration(resolvedCall.candidateDescriptor) as KtFunction?
                    val expectedType = context.get(BindingContext.TYPE, entry.typeReference!!)
                    if (componentFunction != null && expectedType != null) {
                        return ChangeFunctionReturnTypeFix(componentFunction, expectedType)
                    }
                    else
                        return null
                }
            }
        }

        fun createFactoryForHasNextFunctionTypeMismatch(): KotlinSingleIntentionActionFactory {
            return object : KotlinSingleIntentionActionFactory() {
                public override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                    val expression = QuickFixUtil.getParentElementOfType(diagnostic, KtExpression::class.java) ?: error("HAS_NEXT_FUNCTION_TYPE_MISMATCH reported on element that is not within any expression")
                    val context = expression.analyze()
                    val resolvedCall = context.get(BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, expression) ?: return null
                    val hasNextDescriptor = resolvedCall.candidateDescriptor
                    val hasNextFunction = DescriptorToSourceUtils.descriptorToDeclaration(hasNextDescriptor) as KtFunction?
                    if (hasNextFunction != null) {
                        return ChangeFunctionReturnTypeFix(hasNextFunction, hasNextDescriptor.builtIns.booleanType)
                    }
                    else
                        return null
                }
            }
        }

        fun createFactoryForCompareToTypeMismatch(): KotlinSingleIntentionActionFactory {
            return object : KotlinSingleIntentionActionFactory() {
                public override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                    val expression = QuickFixUtil.getParentElementOfType(diagnostic, KtBinaryExpression::class.java) ?: error("COMPARE_TO_TYPE_MISMATCH reported on element that is not within any expression")
                    val context = expression.analyze()
                    val resolvedCall = expression.getResolvedCall(context) ?: return null
                    val compareToDescriptor = resolvedCall.candidateDescriptor
                    val compareTo = DescriptorToSourceUtils.descriptorToDeclaration(compareToDescriptor)
                    if (compareTo !is KtFunction) return null
                    return ChangeFunctionReturnTypeFix(compareTo, compareToDescriptor.builtIns.intType)
                }
            }
        }

        fun createFactoryForReturnTypeMismatchOnOverride(): KotlinIntentionActionsFactory {
            return object : KotlinIntentionActionsFactory() {
                override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
                    val actions = LinkedList<IntentionAction>()

                    val function = QuickFixUtil.getParentElementOfType(diagnostic, KtFunction::class.java)
                    if (function != null) {
                        val descriptor = function.resolveToDescriptor() as FunctionDescriptor

                        val matchingReturnType = QuickFixUtil.findLowerBoundOfOverriddenCallablesReturnTypes(descriptor)
                        if (matchingReturnType != null) {
                            actions.add(ChangeFunctionReturnTypeFix(function, matchingReturnType))
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
                                actions.add(ChangeFunctionReturnTypeFix(overriddenFunction, functionType))
                            }
                        }
                    }
                    return actions
                }
            }
        }

        fun createFactoryForChangingReturnTypeToUnit(): KotlinSingleIntentionActionFactory {
            return object : KotlinSingleIntentionActionFactory() {
                public override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                    val function = QuickFixUtil.getParentElementOfType(diagnostic, KtFunction::class.java) ?: return null
                    return ChangeFunctionReturnTypeFix(function, function.platform.builtIns.unitType)
                }
            }
        }

        fun createFactoryForChangingReturnTypeToNothing(): KotlinSingleIntentionActionFactory {
            return object : KotlinSingleIntentionActionFactory() {
                public override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                    val function = QuickFixUtil.getParentElementOfType(diagnostic, KtFunction::class.java)
                    return if (function == null) null else ChangeFunctionReturnTypeFix(function, function.platform.builtIns.nothingType)
                }
            }
        }
    }
}
