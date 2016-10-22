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

import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.ConvertToBlockBodyIntention
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import java.util.*

internal abstract class ReplacementPerformer<TElement : KtElement>(
        protected val replacement: MutableReplacementCode,
        protected var elementToBeReplaced: TElement
) {
    protected val psiFactory = KtPsiFactory(elementToBeReplaced)

    abstract fun doIt(postProcessing: (PsiChildRange) -> PsiChildRange): TElement
}

internal class AnnotationEntryReplacementPerformer(
        replacement: MutableReplacementCode,
        elementToBeReplaced: KtAnnotationEntry
) : ReplacementPerformer<KtAnnotationEntry>(replacement, elementToBeReplaced) {

    override fun doIt(postProcessing: (PsiChildRange) -> PsiChildRange): KtAnnotationEntry {
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

internal class ExpressionReplacementPerformer(
        replacement: MutableReplacementCode,
        expressionToBeReplaced: KtExpression
) : ReplacementPerformer<KtExpression>(replacement, expressionToBeReplaced) {

    override fun doIt(postProcessing: (PsiChildRange) -> PsiChildRange): KtExpression {
        val insertedStatements = ArrayList<KtExpression>()
        val toInsertedStatementMap = HashMap<KtExpression, KtExpression>()
        for (statement in replacement.statementsBefore) {
            // copy the statement if it can get invalidated by findOrCreateBlockToInsertStatement()
            val statementToUse = if (statement.isPhysical) statement.copy() else statement
            val anchor = findOrCreateBlockToInsertStatement()
            val block = anchor.parent as KtBlockExpression

            val inserted = block.addBefore(statementToUse, anchor) as KtExpression
            block.addBefore(psiFactory.createNewLine(), anchor)
            block.addBefore(psiFactory.createNewLine(), inserted)
            toInsertedStatementMap.put(statement, inserted)
            insertedStatements.add(inserted)
        }

        val replaced = elementToBeReplaced.replace(replacement.mainExpression!!) //TODO: support null here

        for ((statement, actions) in replacement.postInsertionActions) {
            val inserted = toInsertedStatementMap[statement]!!
            actions.forEach { it(inserted) }
        }

        var range = if (insertedStatements.isEmpty()) {
            PsiChildRange.singleElement(replaced)
        }
        else {
            val statement = insertedStatements.first()
            PsiChildRange(statement, replaced.parentsWithSelf.first { it.parent == statement.parent })
        }

        range = postProcessing(range)

        return range.last as KtExpression //TODO: return value not correct!
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

        //TODO
        throw UnsupportedOperationException()
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