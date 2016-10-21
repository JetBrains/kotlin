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

package org.jetbrains.kotlin.idea.replacement

import org.jetbrains.kotlin.idea.analysis.computeTypeInContext
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.scopes.utils.findLocalVariable
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

internal abstract class ConstructedCodeHolder<TElement : KtElement>(
        val replacement: MutableReplacementCode,
        val elementToBeReplaced: TElement,
        val bindingContext: BindingContext
) {
    val psiFactory = KtPsiFactory(elementToBeReplaced)

    abstract fun finish(postProcessing: (PsiChildRange) -> PsiChildRange): TElement
}

internal class ConstructedAnnotationEntryHolder(
        replacement: MutableReplacementCode,
        elementToBeReplaced: KtAnnotationEntry,
        bindingContext: BindingContext
) : ConstructedCodeHolder<KtAnnotationEntry>(replacement, elementToBeReplaced, bindingContext) {

    override fun finish(postProcessing: (PsiChildRange) -> PsiChildRange): KtAnnotationEntry {
        assert(replacement.mainExpression != null)
        assert(replacement.statementsBefore.isEmpty())

        val dummyAnnotationEntry = createByPattern("@Dummy($0)", replacement.mainExpression!!) { psiFactory.createAnnotationEntry(it) }
        val replaced = elementToBeReplaced.replace(dummyAnnotationEntry)
        var range = PsiChildRange.singleElement(replaced)
        range = postProcessing(range)

        assert(range.first == range.last)
        assert(range.first is KtAnnotationEntry)
        val annotationEntry = range.first as KtAnnotationEntry
        val text = annotationEntry.valueArguments.single().getArgumentExpression()!!.text
        return annotationEntry.replaced(psiFactory.createAnnotationEntry("@" + text))
    }
}

internal class ConstructedExpressionHolder(
        replacement: MutableReplacementCode,
        expressionToBeReplaced: KtExpression,
        bindingContext: BindingContext
) : ConstructedCodeHolder<KtExpression>(replacement, expressionToBeReplaced, bindingContext) {

    private data class StatementToInsert<TStatement : KtExpression>(val statement: TStatement, val postProcessing: (TStatement) -> Unit)

    private val statementsToInsert = ArrayList<StatementToInsert<*>>()

    private fun <TStatement : KtExpression> addStatementToInsert(statement: TStatement, postProcessing: (TStatement) -> Unit = {}) {
        statementsToInsert.add(StatementToInsert(statement, postProcessing))
    }

    override fun finish(postProcessing: (PsiChildRange) -> PsiChildRange): KtExpression {
        val insertedStatements = ArrayList<KtExpression>()
        for (toInsert in statementsToInsert.asReversed()) { //TODO: do we need it?
            val block = elementToBeReplaced.parent as KtBlockExpression //TODO

            val inserted = block.addBefore(toInsert.statement, elementToBeReplaced) as KtExpression
            block.addBefore(psiFactory.createNewLine(), elementToBeReplaced)
            insertedStatements.add(inserted)

            @Suppress("UNCHECKED_CAST")
            (toInsert.postProcessing as (KtExpression) -> Unit).invoke(inserted)
        }

        val replaced = elementToBeReplaced.replace(replacement.mainExpression!!) //TODO: support null here

        //TODO: support code.statementsBefore

        var range = if (insertedStatements.isEmpty())
            PsiChildRange.singleElement(replaced)
        else
            PsiChildRange(insertedStatements.first(), replaced)

        range = postProcessing(range)

        return range.last as KtExpression
    }

    fun introduceValue(
            value: KtExpression,
            valueType: KotlinType?,
            usages: Collection<KtExpression>,
            nameSuggestion: String? = null,
            safeCall: Boolean = false
    ) {
        assert(usages.all { replacement.containsStrictlyInside(it) })

        fun replaceUsages(name: Name) {
            val nameInCode = psiFactory.createExpression(name.render())
            for (usage in usages) {
                usage.replace(nameInCode)
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
        fun isNameUsed(name: String) = collectNameUsages(replacement, name).any { nameUsage -> usages.none { it.isAncestor(nameUsage) } }

        if (!safeCall) {
            val block = elementToBeReplaced.parent as? KtBlockExpression
            if (block != null) {
                val resolutionScope = elementToBeReplaced.getResolutionScope(bindingContext, elementToBeReplaced.getResolutionFacade())

                if (usages.isNotEmpty()) {
                    var explicitType: KotlinType? = null
                    if (valueType != null && !ErrorUtils.containsErrorType(valueType)) {
                        val valueTypeWithoutExpectedType = value.computeTypeInContext(
                                resolutionScope,
                                elementToBeReplaced,
                                dataFlowInfo = bindingContext.getDataFlowInfo(elementToBeReplaced)
                        )
                        if (valueTypeWithoutExpectedType == null || ErrorUtils.containsErrorType(valueTypeWithoutExpectedType)) {
                            explicitType = valueType
                        }
                    }

                    val name = suggestName { name ->
                        resolutionScope.findLocalVariable(Name.identifier(name)) == null && !isNameUsed(name)
                    }

                    val declaration = psiFactory.createDeclarationByPattern<KtVariableDeclaration>("val $0 = $1", name, value)
                    addStatementToInsert(declaration,
                                         postProcessing = {
                                             if (explicitType != null) {
                                                 it.setType(explicitType!!)
                                             }
                                         })

                    replaceUsages(name)
                }
                else {
                    addStatementToInsert(value)
                }
                return
            }
        }

        //TODO: handle mainExpression == null and statementsBefore!

        val dot = if (safeCall) "?." else "."

        replacement.mainExpression = if (!isNameUsed("it")) {
            replaceUsages(Name.identifier("it"))
            psiFactory.createExpressionByPattern("$0${dot}let { $1 }", value, replacement.mainExpression!!)
        }
        else {
            val name = suggestName { !isNameUsed(it) }
            replaceUsages(name)
            psiFactory.createExpressionByPattern("$0${dot}let { $1 -> $2 }", value, name, replacement.mainExpression!!)
        }
    }

    private fun collectNameUsages(scope: MutableReplacementCode, name: String): List<KtSimpleNameExpression> {
        return scope.expressions.flatMap { expression ->
            expression.collectDescendantsOfType<KtSimpleNameExpression> { it.getReceiverExpression() == null && it.getReferencedName() == name }
        }
    }
}