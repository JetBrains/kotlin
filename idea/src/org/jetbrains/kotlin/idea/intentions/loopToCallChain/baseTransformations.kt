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

import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange

abstract class ReplaceLoopResultTransformation(override val loop: KtForExpression): ResultTransformation {

    override val commentSavingRange = PsiChildRange.singleElement(loop.unwrapIfLabeled())

    override fun convertLoop(resultCallChain: KtExpression, commentSavingRangeHolder: CommentSavingRangeHolder): KtExpression {
        return loop.unwrapIfLabeled().replaced(resultCallChain)
    }
}

abstract class AssignToVariableResultTransformation(
        override val loop: KtForExpression,
        protected val initialization: VariableInitialization
) : ResultTransformation {

    override val commentSavingRange = PsiChildRange(initialization.initializationStatement, loop.unwrapIfLabeled())

    override fun convertLoop(resultCallChain: KtExpression, commentSavingRangeHolder: CommentSavingRangeHolder): KtExpression {
        initialization.initializer.replace(resultCallChain)

        val loopUnwrapped = loop.unwrapIfLabeled()
        val previousStatement = loopUnwrapped.previousStatement()

        loopUnwrapped.delete()

        if (initialization.variable.isVar && !initialization.variable.hasWriteUsages()) { // change variable to 'val' if possible
            initialization.variable.valOrVarKeyword.replace(KtPsiFactory(initialization.variable).createValKeyword())
        }

        // move initializer to the place where the loop was if needed
        var initializationStatement = initialization.initializationStatement
        if (initializationStatement != previousStatement) {
            val block = initializationStatement.parent
            assert(block is KtBlockExpression)
            val movedInitializationStatement = block.addAfter(initializationStatement, previousStatement) as KtExpression
            block.addAfter(KtPsiFactory(block).createNewLine(), previousStatement)

            commentSavingRangeHolder.remove(initializationStatement)

            initializationStatement.delete()
            initializationStatement = movedInitializationStatement
        }

        return initializationStatement
    }

    companion object {
        fun createDelegated(delegate: ResultTransformation, initialization: VariableInitialization): AssignToVariableResultTransformation {
            return object: AssignToVariableResultTransformation(delegate.loop, initialization) {
                override val presentation: String
                    get() = delegate.presentation

                override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
                    return delegate.generateCode(chainedCallGenerator)
                }
            }
        }
    }
}

class AssignSequenceTransformationResultTransformation(
        private val sequenceTransformation: SequenceTransformation,
        initialization: VariableInitialization
) : AssignToVariableResultTransformation(sequenceTransformation.loop, initialization) {

    override val presentation: String
        get() = sequenceTransformation.presentation

    override fun buildPresentation(prevTransformationsPresentation: String?): String {
        return sequenceTransformation.buildPresentation(prevTransformationsPresentation)
    }

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return sequenceTransformation.generateCode(chainedCallGenerator)
    }
}