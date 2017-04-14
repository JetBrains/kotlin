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

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.ConvertToBlockBodyIntention
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import java.util.*

internal abstract class ReplacementPerformer<TElement : KtElement>(
        protected val codeToInline: MutableCodeToInline,
        protected var elementToBeReplaced: TElement
) {
    protected val psiFactory = KtPsiFactory(elementToBeReplaced)

    abstract fun doIt(postProcessing: (PsiChildRange) -> PsiChildRange): TElement?
}

internal class AnnotationEntryReplacementPerformer(
        codeToInline: MutableCodeToInline,
        elementToBeReplaced: KtAnnotationEntry
) : ReplacementPerformer<KtAnnotationEntry>(codeToInline, elementToBeReplaced) {

    override fun doIt(postProcessing: (PsiChildRange) -> PsiChildRange): KtAnnotationEntry {
        assert(codeToInline.mainExpression != null)
        assert(codeToInline.statementsBefore.isEmpty())

        val dummyAnnotationEntry = createByPattern("@Dummy($0)", codeToInline.mainExpression!!) { psiFactory.createAnnotationEntry(it) }
        val replaced = elementToBeReplaced.replace(dummyAnnotationEntry)

        codeToInline.performPostInsertionActions(listOf(replaced))

        var range = PsiChildRange.singleElement(replaced)
        range = postProcessing(range)

        assert(range.first == range.last)
        assert(range.first is KtAnnotationEntry)
        val annotationEntry = range.first as KtAnnotationEntry
        val text = annotationEntry.valueArguments.single().getArgumentExpression()!!.text
        return annotationEntry.replaced(psiFactory.createAnnotationEntry("@" + text))
    }
}

internal class ExpressionReplacementPerformer(
        codeToInline: MutableCodeToInline,
        expressionToBeReplaced: KtExpression
) : ReplacementPerformer<KtExpression>(codeToInline, expressionToBeReplaced) {

    fun KtExpression.replacedWithStringTemplate(templateExpression: KtStringTemplateExpression): KtExpression? {
        val parent = this.parent

        return if (parent is KtStringTemplateEntryWithExpression
                   // Do not mix raw and non-raw templates
                   && parent.parent.firstChild.text == templateExpression.firstChild.text) {

            val entriesToAdd = templateExpression.entries
            val grandParentTemplateExpression = parent.parent as KtStringTemplateExpression
            val result = if (entriesToAdd.isNotEmpty()) {
                grandParentTemplateExpression.addRangeBefore(entriesToAdd.first(), entriesToAdd.last(), parent)
                val lastNewEntry = parent.prevSibling
                val nextElement = parent.nextSibling
                if (lastNewEntry is KtSimpleNameStringTemplateEntry &&
                    lastNewEntry.expression != null &&
                    !canPlaceAfterSimpleNameEntry(nextElement)) {
                    lastNewEntry.replace(KtPsiFactory(this).createBlockStringTemplateEntry(lastNewEntry.expression!!))
                }
                grandParentTemplateExpression
            }
            else null

            parent.delete()
            result
        }
        else {
            replaced(templateExpression)
        }
    }

    override fun doIt(postProcessing: (PsiChildRange) -> PsiChildRange): KtExpression? {
        val insertedStatements = ArrayList<KtExpression>()
        for (statement in codeToInline.statementsBefore) {
            val statementToUse = statement.copy()
            val anchor = findOrCreateBlockToInsertStatement()
            val block = anchor.parent as KtBlockExpression

            val inserted = block.addBefore(statementToUse, anchor) as KtExpression
            block.addBefore(psiFactory.createNewLine(), anchor)
            block.addBefore(psiFactory.createNewLine(), inserted)
            insertedStatements.add(inserted)
        }

        val mainExpression = codeToInline.mainExpression
        val replaced: PsiElement? = when (mainExpression) {
            is KtStringTemplateExpression -> elementToBeReplaced.replacedWithStringTemplate(mainExpression)
            is KtExpression -> elementToBeReplaced.replaced(mainExpression)
            else -> {
                // NB: Unit is never used as expression
                val stub = elementToBeReplaced.replaced(psiFactory.createExpression("0"))
                val bindingContext = stub.analyze()
                val canDropElementToBeReplaced = !stub.isUsedAsExpression(bindingContext)
                if (canDropElementToBeReplaced) {
                    stub.delete()
                    null
                }
                else {
                    stub.replace(psiFactory.createExpression("Unit"))
                }
            }
        }

        codeToInline.performPostInsertionActions(insertedStatements + listOfNotNull(replaced))

        var range = if (replaced != null) {
            if (insertedStatements.isEmpty()) {
                PsiChildRange.singleElement(replaced)
            }
            else {
                val statement = insertedStatements.first()
                PsiChildRange(statement, replaced.parentsWithSelf.first { it.parent == statement.parent })
            }
        }
        else {
            if (insertedStatements.isEmpty()) {
                PsiChildRange.EMPTY
            }
            else {
                PsiChildRange(insertedStatements.first(), insertedStatements.last())
            }
        }

        range = postProcessing(range)

        return range.last as KtExpression? //TODO: return value not correct!
    }

    /**
     * Returns statement in a block to insert statement before it
     */
    private fun findOrCreateBlockToInsertStatement(): KtExpression {
        //TODO: Sometimes it's not correct because of side effects

        for (element in elementToBeReplaced.parentsWithSelf) {
            val parent = element.parent
            when (element) {
                is KtContainerNodeForControlStructureBody -> { // control statement without block
                    return element.expression!!.replaceWithBlock()
                }

                is KtExpression -> {
                    if (parent is KtWhenEntry) { // when entry without block
                        return element.replaceWithBlock()
                    }

                    if (parent is KtDeclarationWithBody) {
                        withElementToBeReplacedPreserved {
                            ConvertToBlockBodyIntention.convert(parent)
                        }
                        return (parent.bodyExpression as KtBlockExpression).statements.single()
                    }

                    if (parent is KtBlockExpression) return element
                }
            }
        }

        val runExpression = psiFactory.createExpressionByPattern("run { $0 }", elementToBeReplaced) as KtCallExpression
        val runAfterReplacement = elementToBeReplaced.replaced(runExpression)
        val block = runAfterReplacement.lambdaArguments[0].getLambdaExpression().bodyExpression!!
        elementToBeReplaced = block.statements.single()
        return elementToBeReplaced

    }

    private fun KtExpression.replaceWithBlock(): KtExpression {
        val blockExpression = withElementToBeReplacedPreserved {
            this.replaced(KtPsiFactory(this).createSingleStatementBlock(this))
        }
        return blockExpression.statements.single()
    }

    private fun <TElement : KtElement> withElementToBeReplacedPreserved(action: () -> TElement): TElement {
        elementToBeReplaced.putCopyableUserData(ELEMENT_TO_BE_REPLACED_KEY, Unit)
        val result = action()
        elementToBeReplaced = result.findDescendantOfType<KtExpression> { it.getCopyableUserData(ELEMENT_TO_BE_REPLACED_KEY) != null }!!
        elementToBeReplaced.putCopyableUserData(ELEMENT_TO_BE_REPLACED_KEY, null)
        return result
    }
}

private val ELEMENT_TO_BE_REPLACED_KEY = Key<Unit>("ELEMENT_TO_BE_REPLACED_KEY")