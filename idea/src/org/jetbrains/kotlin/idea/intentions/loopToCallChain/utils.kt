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

package org.jetbrains.kotlin.idea.intentions.loopToCallChain

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

fun generateLambda(inputVariable: KtCallableDeclaration, expression: KtExpression): KtLambdaExpression {
    val psiFactory = KtPsiFactory(expression)

    val lambdaExpression = psiFactory.createExpressionByPattern("{ $0 -> $1 }", inputVariable.nameAsSafeName, expression) as KtLambdaExpression

    val isItUsedInside = expression.anyDescendantOfType<KtNameReferenceExpression> {
        it.getQualifiedExpressionForSelector() == null && it.getReferencedName() == "it"
    }

    if (isItUsedInside) return lambdaExpression

    val resolutionScope = inputVariable.getResolutionScope(inputVariable.analyze(BodyResolveMode.FULL), inputVariable.getResolutionFacade())
    val bindingContext = lambdaExpression.analyzeInContext(resolutionScope, contextExpression = inputVariable)
    val lambdaParam = lambdaExpression.valueParameters.single()
    val lambdaParamDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, lambdaParam]
    val usages = lambdaExpression.collectDescendantsOfType<KtNameReferenceExpression> {
        it.mainReference.resolveToDescriptors(bindingContext).singleOrNull() == lambdaParamDescriptor
    }

    val itExpr = psiFactory.createSimpleName("it")
    for (usage in usages) {
        val replaced = usage.replaced(itExpr)

        // we need to copy user data for checkSmartCastsPreserved() to work
        (usage.node as UserDataHolderBase).copyCopyableDataTo(replaced.node as UserDataHolderBase)
    }

    return psiFactory.createExpressionByPattern("{ $0 }", lambdaExpression.bodyExpression!!) as KtLambdaExpression
}

fun generateLambda(expression: KtExpression, vararg inputVariables: KtCallableDeclaration): KtLambdaExpression {
    return KtPsiFactory(expression).buildExpression {
        appendFixedText("{")

        for ((index, variable) in inputVariables.withIndex()) {
            if (index > 0) {
                appendFixedText(",")
            }
            appendName(variable.nameAsSafeName)
        }

        appendFixedText("->")

        appendExpression(expression)

        appendFixedText("}")
    } as KtLambdaExpression
}

data class VariableInitialization(
        val variable: KtProperty,
        val initializationStatement: KtExpression,
        val initializer: KtExpression)

//TODO: we need more correctness checks (if variable is non-local or is local but can be changed by some local functions)
fun KtExpression.detectInitializationBeforeLoop(
        loop: KtForExpression,
        checkNoOtherUsagesInLoop: Boolean
): VariableInitialization? {
    if (this !is KtNameReferenceExpression) return null
    if (getQualifiedExpressionForSelector() != null) return null
    val variable = this.mainReference.resolve() as? KtProperty ?: return null
    val statementBeforeLoop = loop.previousStatement() //TODO: support initialization not right before the loop

    // do not allow any other usages of this variable inside the loop
    if (checkNoOtherUsagesInLoop && variable.countUsages(loop) > 1) return null

    if (statementBeforeLoop == variable) {
        val initializer = variable.initializer ?: return null
        return VariableInitialization(variable, variable, initializer)
    }

    val assignment = statementBeforeLoop?.asAssignment() ?: return null
    if (!assignment.left.isVariableReference(variable)) return null

    val initializer = assignment.right ?: return null
    return VariableInitialization(variable, assignment, initializer)
}

enum class CollectionKind {
    LIST, SET/*, MAP*/
}

fun KtExpression.isSimpleCollectionInstantiation(): CollectionKind? {
    val callExpression = this as? KtCallExpression ?: return null //TODO: it can be qualified too
    if (callExpression.valueArguments.isNotEmpty()) return null
    val bindingContext = callExpression.analyze(BodyResolveMode.PARTIAL)
    val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return null
    val constructorDescriptor = resolvedCall.resultingDescriptor as? ConstructorDescriptor ?: return null
    val classDescriptor = constructorDescriptor.containingDeclaration
    val classFqName = classDescriptor.importableFqName?.asString()
    return when (classFqName) {
        "java.util.ArrayList" -> CollectionKind.LIST
        "java.util.HashSet", "java.util.LinkedHashSet" -> CollectionKind.SET
        else -> null
    }
}

fun canChangeLocalVariableType(variable: KtProperty, newTypeText: String, loop: KtForExpression): Boolean {
    val bindingContext = variable.analyze(BodyResolveMode.FULL)

    // analyze the closest block which is not used as expression
    val block = variable.parents
                        .filterIsInstance<KtBlockExpression>()
                        .firstOrNull { bindingContext[BindingContext.USED_AS_EXPRESSION, it] != true }
                ?: return false

    val KEY = Key<Unit>("KEY")
    block.putCopyableUserData(KEY, Unit)
    variable.putCopyableUserData(KEY, Unit)
    loop.putCopyableUserData(KEY, Unit)

    val fileCopy = block.containingFile.copied()
    val blockCopy: KtBlockExpression
    val variableCopy: KtProperty
    val loopCopy: KtForExpression
    try {
        blockCopy = fileCopy.findDescendantOfType<KtBlockExpression> { it.getCopyableUserData(KEY) != null }!!
        variableCopy = blockCopy.findDescendantOfType<KtProperty> { it.getCopyableUserData(KEY) != null }!!
        loopCopy = blockCopy.findDescendantOfType<KtForExpression> { it.getCopyableUserData(KEY) != null }!!
    }
    finally {
        block.putCopyableUserData(KEY, null)
        variable.putCopyableUserData(KEY, null)
        loop.putCopyableUserData(KEY, null)
    }

    variableCopy.typeReference = KtPsiFactory(block).createType(newTypeText)

    val resolutionScope = block.getResolutionScope(bindingContext, block.getResolutionFacade())
    val newBindingContext = blockCopy.analyzeInContext(scope = resolutionScope,
                                                       contextExpression = block,
                                                       dataFlowInfo = bindingContext.getDataFlowInfo(block),
                                                       trace = DelegatingBindingTrace(bindingContext, "Temporary trace"))
    //TODO: what if there were errors before?
    return newBindingContext.diagnostics.none { it.severity == Severity.ERROR && !loopCopy.isAncestor(it.psiElement) }
}

private val NO_SIDE_EFFECT_STANDARD_CLASSES = setOf(
        "java.util.ArrayList",
        "java.util.LinkedList",
        "java.util.HashSet",
        "java.util.LinkedHashSet",
        "java.util.HashMap",
        "java.util.LinkedHashMap"
)

fun KtExpression.hasNoSideEffect(): Boolean {
    val bindingContext = analyze(BodyResolveMode.PARTIAL)
    if (ConstantExpressionEvaluator.getConstant(this, bindingContext) != null) return true

    val callExpression = this as? KtCallExpression ?: return false//TODO: it can be qualified too
    if (callExpression.valueArguments.any { it.getArgumentExpression()?.hasNoSideEffect() == false }) return false

    val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return false
    val constructorDescriptor = resolvedCall.resultingDescriptor as? ConstructorDescriptor ?: return false
    val classDescriptor = constructorDescriptor.containingDeclaration
    val classFqName = classDescriptor.importableFqName?.asString()
    return classFqName in NO_SIDE_EFFECT_STANDARD_CLASSES
}
