/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.slicer

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector.Access
import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.light.LightMemberReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.slicer.JavaSliceUsage
import com.intellij.slicer.SliceUsage
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.cfg.pseudocode.getContainingPseudocode
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ReturnValueInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverse
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.core.isOverridable
import org.jetbrains.kotlin.idea.findUsages.KotlinFunctionFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinPropertyFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.processAllExactUsages
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinValVar
import org.jetbrains.kotlin.idea.refactoring.changeSignature.toValVar
import org.jetbrains.kotlin.idea.references.KtPropertyDelegationMethodsReference
import org.jetbrains.kotlin.idea.references.ReferenceAccess
import org.jetbrains.kotlin.idea.references.readWriteAccessWithFullExpression
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchOverriders
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReadWriteAccessDetector
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

private fun PsiElement.processHierarchyDownward(scope: SearchScope, processor: PsiElement.() -> Unit) {
    processor()
    HierarchySearchRequest(this, scope).searchOverriders().forEach {
        it.namedUnwrappedElement?.processor()
    }
}

private fun KtDeclaration.processHierarchyUpward(scope: AnalysisScope, processor: PsiElement.() -> Unit) {
    processor()
    val descriptor = unsafeResolveToDescriptor() as? CallableMemberDescriptor ?: return
    DescriptorUtils
        .getAllOverriddenDescriptors(descriptor)
        .asSequence()
        .mapNotNull { it.source.getPsi() }
        .filter { scope.contains(it) }
        .toList()
            .forEach(processor)
}

private fun KtFunction.processCalls(scope: SearchScope, processor: (UsageInfo) -> Unit) {
    processAllExactUsages(
            {
                KotlinFunctionFindUsagesOptions(project).apply {
                    isSearchForTextOccurrences = false
                    isSkipImportStatements = true
                    searchScope = scope.intersectWith(useScope)
                }
            },
            processor
    )
}

private enum class AccessKind {
    READ_ONLY, WRITE_ONLY, WRITE_WITH_OPTIONAL_READ, READ_OR_WRITE
}

private fun KtDeclaration.processVariableAccesses(
        scope: SearchScope,
        kind: AccessKind,
        processor: (UsageInfo) -> Unit
) {
    processAllExactUsages(
            {
                KotlinPropertyFindUsagesOptions(project).apply {
                    isReadAccess = kind == AccessKind.READ_ONLY || kind == AccessKind.READ_OR_WRITE
                    isWriteAccess = kind == AccessKind.WRITE_ONLY || kind == AccessKind.WRITE_WITH_OPTIONAL_READ || kind == AccessKind.READ_OR_WRITE
                    isReadWriteAccess = kind == AccessKind.WRITE_WITH_OPTIONAL_READ || kind == AccessKind.READ_OR_WRITE
                    isSearchForTextOccurrences = false
                    isSkipImportStatements = true
                    searchScope = scope.intersectWith(useScope)
                }
            },
            processor
    )
}

private fun KtParameter.canProcess() = !isVarArg

abstract class Slicer(
        protected val element: KtExpression,
        protected val processor: Processor<SliceUsage>,
        protected val parentUsage: KotlinSliceUsage
) {
    protected class PseudocodeCache {
        private val computedPseudocodes = HashMap<KtElement, Pseudocode>()

        operator fun get(element: KtElement): Pseudocode? {
            val container = element.containingDeclarationForPseudocode ?: return null
            return computedPseudocodes.getOrPut(container) {
                container.getContainingPseudocode(container.analyzeWithContent())?.apply { computedPseudocodes[container] = this } ?: return null
            }
        }
    }

    protected val pseudocodeCache = PseudocodeCache()

    protected fun PsiElement.passToProcessor(
            lambdaLevel: Int = parentUsage.lambdaLevel,
            forcedExpressionMode: Boolean = false
    ) {
        processor.process(KotlinSliceUsage(this, parentUsage, lambdaLevel, forcedExpressionMode))
    }

    abstract fun processChildren()
}

class InflowSlicer(
        element: KtExpression,
        processor: Processor<SliceUsage>,
        parentUsage: KotlinSliceUsage
) : Slicer(element, processor, parentUsage) {
    private fun PsiElement.processHierarchyDownwardAndPass() {
        processHierarchyDownward(parentUsage.scope.toSearchScope()) { passToProcessor() }
    }

    private fun PsiElement.passToProcessorAsValue(lambdaLevel: Int = parentUsage.lambdaLevel) = passToProcessor(lambdaLevel, true)

    private fun KtDeclaration.processAssignments(accessSearchScope: SearchScope) {
        processVariableAccesses(accessSearchScope, AccessKind.WRITE_WITH_OPTIONAL_READ) body@ {
            val refElement = it.element ?: return@body
            val refParent = refElement.parent

            val rhsValue = when {
                refElement is KtExpression -> {
                    val (accessKind, accessExpression) = refElement.readWriteAccessWithFullExpression(true)
                    if (accessKind == ReferenceAccess.WRITE && accessExpression is KtBinaryExpression && accessExpression.operationToken == KtTokens.EQ) {
                        accessExpression.right
                    }
                    else {
                        accessExpression
                    }
                }

                refParent is PsiCall -> refParent.argumentList?.expressions?.getOrNull(0)

                else -> null
            }
            rhsValue?.passToProcessorAsValue()
        }
    }

    private fun KtPropertyAccessor.processBackingFieldAssignments() {
        forEachDescendantOfType<KtBinaryExpression> body@ {
            if (it.operationToken != KtTokens.EQ) return@body
            val lhs = it.left?.let { KtPsiUtil.safeDeparenthesize(it) } ?: return@body
            val rhs = it.right ?: return@body
            if (!lhs.isBackingFieldReference()) return@body
            rhs.passToProcessor()
        }
    }

    private fun KtProperty.processPropertyAssignments() {
        val analysisScope = parentUsage.scope.toSearchScope()
        val accessSearchScope = if (isVar) analysisScope
        else {
            val containerScope = getStrictParentOfType<KtDeclaration>()?.let { LocalSearchScope(it) } ?: return
            analysisScope.intersectWith(containerScope)
        }
        processAssignments(accessSearchScope)
    }

    private fun KtProperty.processProperty() {
        val bindingContext by lazy { analyzeWithContent() }

        if (hasDelegateExpression()) {
            val getter = (unsafeResolveToDescriptor() as VariableDescriptorWithAccessors).getter
            val delegateGetterResolvedCall = getter?.let { bindingContext[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, it] }
            delegateGetterResolvedCall?.resultingDescriptor?.source?.getPsi()?.passToProcessor()
            return
        }

        initializer?.passToProcessor()

        getter?.processFunction()

        val isDefaultGetter = getter?.bodyExpression == null
        val isDefaultSetter = setter?.bodyExpression == null
        if (isDefaultGetter) {
            if (isDefaultSetter) {
                processPropertyAssignments()
            }
            else {
                setter!!.processBackingFieldAssignments()
            }
        }
    }

    private fun KtParameter.processParameter() {
        if (!canProcess()) return

        val function = ownerFunction ?: return
        if (function.isOverridable()) return

        if (function is KtPropertyAccessor && function.isSetter) {
            function.property.processPropertyAssignments()
            return
        }

        if (function is KtNamedFunction
            && function.name == OperatorNameConventions.SET_VALUE.asString()
            && function.hasModifier(KtTokens.OPERATOR_KEYWORD)) {

            ReferencesSearch
                    .search(function, parentUsage.scope.toSearchScope())
                    .filterIsInstance<KtPropertyDelegationMethodsReference>()
                    .forEach { (it.element?.parent as? KtProperty)?.processPropertyAssignments() }
        }

        val parameterDescriptor = resolveToParameterDescriptorIfAny(BodyResolveMode.FULL) ?: return

        (function as? KtFunction)?.processCalls(parentUsage.scope.toSearchScope()) body@ {
            val refElement = it.element ?: return@body
            val refParent = refElement.parent

            val argumentExpression = when {
                refElement is KtExpression -> {
                    val callElement = refElement.getParentOfTypeAndBranch<KtCallElement> { calleeExpression } ?: return@body
                    val resolvedCall = callElement.resolveToCall() ?: return@body
                    val resolvedArgument = resolvedCall.valueArguments[parameterDescriptor] ?: return@body
                    when (resolvedArgument) {
                        is DefaultValueArgument -> defaultValue
                        is ExpressionValueArgument -> resolvedArgument.valueArgument?.getArgumentExpression()
                        else -> null
                    }
                }

                refParent is PsiCall -> refParent.argumentList?.expressions?.getOrNull(this@processParameter.parameterIndex())

                else -> null
            }
            argumentExpression?.passToProcessorAsValue()
        }

        if (valOrVarKeyword.toValVar() == KotlinValVar.Var) {
            processAssignments(parentUsage.scope.toSearchScope())
        }
    }

    private fun KtDeclarationWithBody.processFunction() {
        val bodyExpression = bodyExpression ?: return
        val pseudocode = pseudocodeCache[bodyExpression] ?: return
        pseudocode.traverse(TraversalOrder.FORWARD) { instr ->
            if (instr is ReturnValueInstruction && instr.subroutine == this) {
                (instr.returnExpressionIfAny?.returnedExpression ?: instr.element as? KtExpression)?.passToProcessorAsValue()
            }
        }
    }

    private fun Instruction.passInputsToProcessor() {
        inputValues.forEach {
            if (it.createdAt != null) {
                it.element?.passToProcessorAsValue()
            }
        }
    }

    private fun KtExpression.isBackingFieldReference(): Boolean {
        return this is KtSimpleNameExpression &&
               getReferencedName() == SyntheticFieldDescriptor.NAME.asString() &&
               resolveToCall()?.resultingDescriptor is SyntheticFieldDescriptor
    }

    private fun KtExpression.processExpression() {
        val lambda = when (this) {
            is KtLambdaExpression -> functionLiteral
            is KtNamedFunction -> if (name == null) this else null
            else -> null
        }
        if (lambda != null) {
            if (parentUsage.lambdaLevel > 0) {
                lambda.passToProcessor(parentUsage.lambdaLevel - 1)
            }
            return
        }

        val pseudocode = pseudocodeCache[this] ?: return
        val expressionValue = pseudocode.getElementValue(this) ?: return
        val createdAt = expressionValue.createdAt
        when (createdAt) {
            is ReadValueInstruction -> {
                if (createdAt.target == AccessTarget.BlackBox) {
                    val originalElement = expressionValue.element as? KtExpression ?: return
                    if (originalElement != this) {
                        originalElement.processExpression()
                    }
                    return
                }
                val accessedDescriptor = createdAt.target.accessedDescriptor ?: return
                val accessedDeclaration = accessedDescriptor.source.getPsi() ?: return
                if (accessedDescriptor is SyntheticFieldDescriptor) {
                    val property = accessedDeclaration as? KtProperty ?: return
                    if (accessedDescriptor.propertyDescriptor.setter?.isDefault ?: true) {
                        property.processPropertyAssignments()
                    }
                    else {
                        property.setter?.processBackingFieldAssignments()
                    }
                    return
                }
                accessedDeclaration.processHierarchyDownwardAndPass()
            }

            is MergeInstruction -> createdAt.passInputsToProcessor()

            is MagicInstruction -> when (createdAt.kind) {
                MagicKind.NOT_NULL_ASSERTION, MagicKind.CAST -> createdAt.passInputsToProcessor()
                MagicKind.BOUND_CALLABLE_REFERENCE, MagicKind.UNBOUND_CALLABLE_REFERENCE -> {
                    val callableRefExpr = expressionValue.element as? KtCallableReferenceExpression ?: return
                    val referencedDescriptor = analyze()[BindingContext.REFERENCE_TARGET, callableRefExpr.callableReference] ?: return
                    val referencedDeclaration = (referencedDescriptor as? DeclarationDescriptorWithSource)?.source?.getPsi() ?: return
                    referencedDeclaration.passToProcessor(parentUsage.lambdaLevel - 1)
                }
                else -> return
            }

            is CallInstruction -> {
                val resolvedCall = createdAt.resolvedCall
                val resultingDescriptor = resolvedCall.resultingDescriptor
                if (resultingDescriptor is FunctionInvokeDescriptor) {
                    (resolvedCall.dispatchReceiver as? ExpressionReceiver)?.expression?.passToProcessorAsValue(parentUsage.lambdaLevel + 1)
                }
                else {
                    resultingDescriptor.source.getPsi()?.processHierarchyDownwardAndPass()
                }
            }
        }
    }

    override fun processChildren() {
        if (parentUsage.forcedExpressionMode) return element.processExpression()

        when (element) {
            is KtProperty -> element.processProperty()
            is KtParameter -> element.processParameter()
            is KtDeclarationWithBody -> element.processFunction()
            else -> element.processExpression()
        }
    }
}

class OutflowSlicer(
        element: KtExpression,
        processor: Processor<SliceUsage>,
        parentUsage: KotlinSliceUsage
) : Slicer(element, processor, parentUsage) {
    private fun KtDeclaration.processVariable() {
        processHierarchyUpward(parentUsage.scope) {
            if (this is KtParameter && !canProcess()) return@processHierarchyUpward

            val withDereferences = parentUsage.params.showInstanceDereferences
            val accessKind = if (withDereferences) AccessKind.READ_OR_WRITE else AccessKind.READ_ONLY
            (this as? KtDeclaration)?.processVariableAccesses(parentUsage.scope.toSearchScope(), accessKind) body@ {
                val refElement = it.element
                if (refElement !is KtExpression) {
                    refElement?.passToProcessor()
                    return@body
                }

                val refExpression = KtPsiUtil.safeDeparenthesize(refElement)
                if (withDereferences) {
                    refExpression.processDereferences()
                }
                if (!withDereferences || KotlinReadWriteAccessDetector.INSTANCE.getExpressionAccess(refExpression) == Access.Read) {
                    refExpression.passToProcessor()
                }
            }
        }
    }

    private fun PsiElement.getCallElementForExactCallee(): PsiElement? {
        if (this is KtArrayAccessExpression) return this

        val operationRefExpr = getNonStrictParentOfType<KtOperationReferenceExpression>()
        if (operationRefExpr != null) return operationRefExpr.parent as? KtOperationExpression

        val parentCall = getParentOfTypeAndBranch<KtCallElement> { calleeExpression } ?: return null
        val callee = parentCall.calleeExpression?.let { KtPsiUtil.safeDeparenthesize(it) }
        if (callee == this || callee is KtConstructorCalleeExpression && callee.isAncestor(this, true)) return parentCall

        return null
    }

    private fun PsiElement.getCallableReferenceForExactCallee(): KtCallableReferenceExpression? {
        val callableRef = getParentOfTypeAndBranch<KtCallableReferenceExpression> { callableReference } ?: return null
        val callee = KtPsiUtil.safeDeparenthesize(callableRef.callableReference)
        return if (callee == this) callableRef else null
    }

    private fun KtFunction.processFunction() {
        if (this is KtConstructor<*> || this is KtNamedFunction && name != null) {
            processHierarchyUpward(parentUsage.scope) {
                if (this is KtFunction) {
                    processCalls(parentUsage.scope.toSearchScope()) {
                        val refElement = it.element
                        when {
                            refElement == null -> (it.reference as? LightMemberReference)?.element?.passToProcessor()
                            refElement is KtExpression -> {
                                refElement.getCallElementForExactCallee()?.passToProcessor()
                                refElement.getCallableReferenceForExactCallee()?.passToProcessor(parentUsage.lambdaLevel + 1)
                            }
                            else -> refElement.passToProcessor()
                        }
                    }
                }
                else if (this is PsiMethod && language == JavaLanguage.INSTANCE) {
                    // todo: work around the bug in JavaSliceProvider.transform()
                    processor.process(JavaSliceUsage.createRootUsage(this, parentUsage.params))
                }
                else {
                    passToProcessor()
                }
            }
            return
        }

        val funExpression = when (this) {
                                is KtFunctionLiteral -> parent as? KtLambdaExpression
                                is KtNamedFunction -> this
                                else -> null
                            } ?: return
        (funExpression as PsiElement).passToProcessor(parentUsage.lambdaLevel + 1, true)
    }

    private fun processDereferenceIsNeeded(
            expression: KtExpression,
            pseudoValue: PseudoValue,
            instr: InstructionWithReceivers
    ) {
        if (!parentUsage.params.showInstanceDereferences) return

        val receiver = instr.receiverValues[pseudoValue]
        val resolvedCall = when (instr) {
                               is CallInstruction -> instr.resolvedCall
                               is ReadValueInstruction -> (instr.target as? AccessTarget.Call)?.resolvedCall
                               else -> null
                           } ?: return

        if (receiver != null && resolvedCall.dispatchReceiver == receiver) {
            processor.process(KotlinSliceDereferenceUsage(expression, parentUsage, parentUsage.lambdaLevel))
        }
    }

    private fun KtExpression.processPseudocodeUsages(processor: (PseudoValue, Instruction) -> Unit) {
        val pseudocode = pseudocodeCache[this] ?: return
        val pseudoValue = pseudocode.getElementValue(this) ?: return
        pseudocode.getUsages(pseudoValue).forEach { processor(pseudoValue, it) }
    }

    private fun KtExpression.processDereferences() {
        processPseudocodeUsages { pseudoValue, instr ->
            when (instr) {
                is ReadValueInstruction -> processDereferenceIsNeeded(this, pseudoValue, instr)
                is CallInstruction -> processDereferenceIsNeeded(this, pseudoValue, instr)
            }
        }
    }

    private fun KtExpression.processExpression() {
        processPseudocodeUsages { pseudoValue, instr ->
            when (instr) {
                is WriteValueInstruction -> instr.target.accessedDescriptor?.source?.getPsi()?.passToProcessor()
                is CallInstruction -> {
                    if (parentUsage.lambdaLevel > 0 && instr.receiverValues[pseudoValue] != null) {
                        instr.element.passToProcessor(parentUsage.lambdaLevel - 1)
                    }
                    else {
                        instr.arguments[pseudoValue]?.source?.getPsi()?.passToProcessor()
                    }
                }
                is ReturnValueInstruction -> instr.subroutine.passToProcessor()
                is MagicInstruction -> when (instr.kind) {
                    MagicKind.NOT_NULL_ASSERTION, MagicKind.CAST -> instr.outputValue.element?.passToProcessor()
                    else -> { }
                }
            }
        }
    }

    override fun processChildren() {
        if (parentUsage.forcedExpressionMode) return element.processExpression()

        when (element) {
            is KtProperty, is KtParameter -> (element as KtDeclaration).processVariable()
            is KtFunction -> element.processFunction()
            is KtPropertyAccessor -> if (element.isGetter) {
                element.property.processVariable()
            }
            else -> element.processExpression()
        }
    }
}