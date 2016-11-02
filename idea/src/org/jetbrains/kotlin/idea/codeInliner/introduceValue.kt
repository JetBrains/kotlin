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

package org.jetbrains.kotlin.idea.codeInliner

import org.jetbrains.kotlin.idea.analysis.computeTypeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.findLocalVariable
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.check

/**
 * Modifies [MutableCodeToInline] introducing a variable initialized by [value] and replacing all of [usages] with its use.
 * The variable must be initialized (and so the value is calculated) before any other code in [MutableCodeToInline].
 * @param value Value to use for variable initialization
 * @param valueType Type of the value
 * @param usages Usages to be replaced. This collection can be empty and in this case the actual variable is not needed.
 * But the expression [value] must be calculated because it may have side effects.
 * @param expressionToBeReplaced Expression to be replaced by the [MutableCodeToInline].
 * @param nameSuggestion Name suggestion for the variable.
 * @param safeCall If true, then the whole code must not be executed if the [value] evaluates to null.
 */
internal fun MutableCodeToInline.introduceValue(
        value: KtExpression,
        valueType: KotlinType?,
        usages: Collection<KtExpression>,
        expressionToBeReplaced: KtExpression,
        nameSuggestion: String? = null,
        safeCall: Boolean = false
) {
    assert(usages.all { this.containsStrictlyInside(it) })

    val psiFactory = KtPsiFactory(value)

    val bindingContext = expressionToBeReplaced.analyze(BodyResolveMode.FULL)

    fun replaceUsages(name: Name) {
        val nameInCode = psiFactory.createExpression(name.render())
        for (usage in usages) {
            // there can be parenthesis around the expression which will become unnecessary
            val usageToReplace = (usage.parent as? KtParenthesizedExpression) ?: usage
            usageToReplace.replace(nameInCode)
        }
    }

    fun suggestName(validator: (String) -> Boolean): Name {
        val name = if (nameSuggestion != null)
            KotlinNameSuggester.suggestNameByName(nameSuggestion, validator)
        else
            KotlinNameSuggester.suggestNamesByExpressionOnly(value, bindingContext, validator, "t").first()
        return Name.identifier(name)
    }

    // checks that name is used (without receiver) inside code being constructed but not inside usages that will be replaced
    fun isNameUsed(name: String) = collectNameUsages(this, name).any { nameUsage -> usages.none { it.isAncestor(nameUsage) } }

    if (!safeCall) {
        if (usages.isNotEmpty()) {
            val resolutionScope = expressionToBeReplaced.getResolutionScope(bindingContext, expressionToBeReplaced.getResolutionFacade())

            val name = suggestName { name ->
                resolutionScope.findLocalVariable(Name.identifier(name)) == null && !isNameUsed(name)
            }

            val declaration = psiFactory.createDeclarationByPattern<KtVariableDeclaration>("val $0 = $1", name, value)
            statementsBefore.add(0, declaration)

            val explicitType = valueType?.check {
                variableNeedsExplicitType(value, valueType, expressionToBeReplaced, resolutionScope, bindingContext)
            }
            if (explicitType != null) {
                addPostInsertionAction(declaration) { it.setType(explicitType) }
            }

            replaceUsages(name)
        }
        else {
            statementsBefore.add(0, value)
        }
    }
    else {
        val useIt = !isNameUsed("it")
        val name = if (useIt) Name.identifier("it") else suggestName { !isNameUsed(it) }
        replaceUsages(name)

        mainExpression = psiFactory.buildExpression {
            appendExpression(value)
            appendFixedText("?.let{")

            if (!useIt) {
                appendName(name)
                appendFixedText("->")
            }

            for (statement in statementsBefore) {
                appendExpression(statement)
                appendFixedText("\n")
            }

            if (mainExpression != null) {
                appendExpression(mainExpression)
            }

            appendFixedText("}")
        }
        statementsBefore.clear()
    }
}

private fun variableNeedsExplicitType(
        initializer: KtExpression,
        initializerType: KotlinType,
        context: KtExpression,
        resolutionScope: LexicalScope,
        bindingContext: BindingContext
): Boolean {
    if (ErrorUtils.containsErrorType(initializerType)) return false
    val valueTypeWithoutExpectedType = initializer.computeTypeInContext(
            resolutionScope,
            context,
            dataFlowInfo = bindingContext.getDataFlowInfoBefore(context)
    )
    return valueTypeWithoutExpectedType == null || ErrorUtils.containsErrorType(valueTypeWithoutExpectedType)
}

private fun collectNameUsages(scope: MutableCodeToInline, name: String): List<KtSimpleNameExpression> {
    return scope.expressions.flatMap { expression ->
        expression.collectDescendantsOfType<KtSimpleNameExpression> { it.getReceiverExpression() == null && it.getReferencedName() == name }
    }
}

