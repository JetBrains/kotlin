package org.jetbrains.kotlin.idea.slicer

import com.intellij.psi.PsiCall
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.slicer.SliceUsage
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ReturnValueInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverse
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinValVar
import org.jetbrains.kotlin.idea.refactoring.changeSignature.toValVar
import org.jetbrains.kotlin.idea.references.KtPropertyDelegationMethodsReference
import org.jetbrains.kotlin.idea.references.ReferenceAccess
import org.jetbrains.kotlin.idea.references.readWriteAccessWithFullExpression
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchOverriders
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parameterIndex
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.util.OperatorNameConventions

class InflowSlicer(
    element: KtExpression,
    processor: Processor<SliceUsage>,
    parentUsage: KotlinSliceUsage
) : Slicer(element, processor, parentUsage) {
    private fun PsiElement.processHierarchyDownwardAndPass() {
        processHierarchyDownward(parentUsage.scope.toSearchScope()) { passToProcessor() }
    }

    private fun PsiElement.processHierarchyDownward(scope: SearchScope, processor: PsiElement.() -> Unit) {
        processor()
        HierarchySearchRequest(this, scope).searchOverriders().forEach {
            it.namedUnwrappedElement?.processor()
        }
    }

    private fun PsiElement.passToProcessorAsValue(lambdaLevel: Int = parentUsage.lambdaLevel) = passToProcessor(lambdaLevel, true)

    private fun processAssignments(variableDeclaration: KtCallableDeclaration, accessSearchScope: SearchScope) {
        processVariableAccesses(variableDeclaration, accessSearchScope, AccessKind.WRITE_WITH_OPTIONAL_READ) body@{
            val refElement = it.element ?: return@body
            val refParent = refElement.parent

            val rhsValue = when {
                refElement is KtExpression -> {
                    val (accessKind, accessExpression) = refElement.readWriteAccessWithFullExpression(true)
                    if (accessKind == ReferenceAccess.WRITE && accessExpression is KtBinaryExpression && accessExpression.operationToken == KtTokens.EQ) {
                        accessExpression.right
                    } else {
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
        forEachDescendantOfType<KtBinaryExpression> body@{
            if (it.operationToken != KtTokens.EQ) return@body
            val lhs = it.left?.let { expression -> KtPsiUtil.safeDeparenthesize(expression) } ?: return@body
            val rhs = it.right ?: return@body
            if (!lhs.isBackingFieldReference()) return@body
            rhs.passToProcessor()
        }
    }

    private fun KtProperty.processPropertyAssignments() {
        val analysisScope = parentUsage.scope.toSearchScope()
        val accessSearchScope = if (isVar) {
            analysisScope
        } else {
            val containerScope = getStrictParentOfType<KtDeclaration>()?.let { LocalSearchScope(it) } ?: return
            analysisScope.intersectWith(containerScope)
        }
        processAssignments(this, accessSearchScope)
    }

    private fun processProperty(property: KtProperty) {
        val bindingContext by lazy { property.analyzeWithContent() }

        if (property.hasDelegateExpression()) {
            val getter = (property.unsafeResolveToDescriptor() as VariableDescriptorWithAccessors).getter
            val delegateGetterResolvedCall = getter?.let { bindingContext[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, it] }
            delegateGetterResolvedCall?.resultingDescriptor?.originalSource?.getPsi()?.passToProcessor()
            return
        }

        property.initializer?.passToProcessor()

        property.getter?.processBody()

        val isDefaultGetter = property.getter?.bodyExpression == null
        val isDefaultSetter = property.setter?.bodyExpression == null
        if (isDefaultGetter) {
            if (isDefaultSetter) {
                property.processPropertyAssignments()
            } else {
                property.setter!!.processBackingFieldAssignments()
            }
        }
    }

    private fun processParameter(parameter: KtParameter, includeOverriders: Boolean) {
        if (!parameter.canProcess()) return

        val function = parameter.ownerFunction ?: return

        if (function is KtPropertyAccessor && function.isSetter) {
            function.property.processPropertyAssignments()
            return
        }

        if (function is KtNamedFunction
            && function.name == OperatorNameConventions.SET_VALUE.asString()
            && function.hasModifier(KtTokens.OPERATOR_KEYWORD)
        ) {

            ReferencesSearch.search(function, parentUsage.scope.toSearchScope())
                .filterIsInstance<KtPropertyDelegationMethodsReference>()
                .forEach { (it.element.parent as? KtProperty)?.processPropertyAssignments() }
        }

        val parameterDescriptor = parameter.resolveToParameterDescriptorIfAny(BodyResolveMode.FULL) ?: return

        (function as? KtFunction)?.processCalls(parentUsage.scope.toSearchScope(), includeOverriders) body@{
            val refElement = it.element ?: return@body
            val refParent = refElement.parent

            val argumentExpression = when {
                refElement is KtExpression -> {
                    val callElement = refElement.getParentOfTypeAndBranch<KtCallElement> { calleeExpression } ?: return@body
                    val resolvedCall = callElement.resolveToCall() ?: return@body
                    val callParameterDescriptor = resolvedCall.resultingDescriptor.valueParameters[parameterDescriptor.index]
                    val resolvedArgument = resolvedCall.valueArguments[callParameterDescriptor] ?: return@body
                    when (resolvedArgument) {
                        is DefaultValueArgument -> parameter.defaultValue
                        is ExpressionValueArgument -> resolvedArgument.valueArgument?.getArgumentExpression()
                        else -> null
                    }
                }

                refParent is PsiCall -> refParent.argumentList?.expressions?.getOrNull(parameter.parameterIndex())

                else -> null
            }
            argumentExpression?.passToProcessorAsValue()
        }

        if (parameter.valOrVarKeyword.toValVar() == KotlinValVar.Var) {
            processAssignments(parameter, parentUsage.scope.toSearchScope())
        }
    }

    private fun KtDeclarationWithBody.processBody() {
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

    private fun processExpression(expression: KtExpression) {
        val lambda = when (expression) {
            is KtLambdaExpression -> expression.functionLiteral
            is KtNamedFunction -> if (expression.name == null) expression else null
            else -> null
        }
        if (lambda != null) {
            if (parentUsage.lambdaLevel > 0) {
                lambda.passToProcessor(parentUsage.lambdaLevel - 1)
            }
            return
        }

        val pseudocode = pseudocodeCache[expression] ?: return
        val expressionValue = pseudocode.getElementValue(expression) ?: return
        when (val createdAt = expressionValue.createdAt) {
            is ReadValueInstruction -> {
                if (createdAt.target == AccessTarget.BlackBox) {
                    val originalElement = expressionValue.element as? KtExpression ?: return
                    if (originalElement != expression) {
                        processExpression(originalElement)
                    }
                    return
                }
                val accessedDescriptor = createdAt.target.accessedDescriptor ?: return
                val accessedDeclaration = accessedDescriptor.originalSource.getPsi() ?: return
                if (accessedDescriptor is SyntheticFieldDescriptor) {
                    val property = accessedDeclaration as? KtProperty ?: return
                    if (accessedDescriptor.propertyDescriptor.setter?.isDefault != false) {
                        property.processPropertyAssignments()
                    } else {
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
                    val callableRefExpr = expressionValue.element as? KtCallableReferenceExpression
                        ?: return
                    val referencedDescriptor = expression.analyze()[BindingContext.REFERENCE_TARGET, callableRefExpr.callableReference] ?: return
                    val referencedDeclaration = (referencedDescriptor as? DeclarationDescriptorWithSource)?.originalSource?.getPsi() ?: return
                    referencedDeclaration.passToProcessor(parentUsage.lambdaLevel - 1)
                }
                else -> return
            }

            is CallInstruction -> {
                val resolvedCall = createdAt.resolvedCall
                val resultingDescriptor = resolvedCall.resultingDescriptor
                if (resultingDescriptor is FunctionInvokeDescriptor) {
                    (resolvedCall.dispatchReceiver as? ExpressionReceiver)?.expression?.passToProcessorAsValue(parentUsage.lambdaLevel + 1)
                } else {
                    resultingDescriptor.originalSource.getPsi()?.processHierarchyDownwardAndPass()
                }
            }
        }
    }

    override fun processChildren() {
        if (parentUsage.forcedExpressionMode) return processExpression(element)

        when (element) {
            is KtProperty -> processProperty(element)
            // for parameter, we include overriders only when the feature is invoked on parameter itself
            is KtParameter -> processParameter(parameter = element, includeOverriders = parentUsage.parent == null)
            is KtDeclarationWithBody -> element.processBody()
            else -> processExpression(element)
        }
    }
}