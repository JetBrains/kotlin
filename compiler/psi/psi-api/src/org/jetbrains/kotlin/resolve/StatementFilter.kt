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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil

/**
 * Selects which statements of a block should be taken into account (for example, during analysis). The base
 * implementation applies no filtering; subclasses may restrict the considered statements by overriding [filter].
 */
open class StatementFilter {
    /**
     * A predicate that returns `true` for the statements to keep, or `null` to keep all statements.
     */
    open val filter: ((KtExpression) -> Boolean)?
        get() = null

    companion object {
        /**
         * A [StatementFilter] that applies no filtering (keeps every statement).
         */
        @JvmField
        val NONE = object : StatementFilter() {
            override fun toString() = "NONE"
        }
    }
}

/**
 * Returns the statements of [block] that pass this filter, or all of them if this filter keeps everything.
 */
fun StatementFilter.filterStatements(block: KtBlockExpression): List<KtExpression> {
    if (filter == null || block is KtPsiUtil.KtExpressionWrapper) return block.statements
    return block.statements.filter { filter!!(it) }
}

/**
 * Returns the last statement of [block] that passes this filter, or `null` if none do.
 */
fun StatementFilter.getLastStatementInABlock(block: KtBlockExpression) = filterStatements(block).lastOrNull()