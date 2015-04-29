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

import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetBlockExpression
import org.jetbrains.kotlin.psi.JetPsiUtil

public open class StatementFilter {
    companion object {
        public val NONE: StatementFilter = StatementFilter()
    }

    public open val filter: ((JetElement) -> Boolean)?
        get() = null

    override fun toString(): String {
        return (this.javaClass).getName()
    }
}

fun StatementFilter.filterStatements(block: JetBlockExpression): List<JetElement> {
    if (filter == null || block is JetPsiUtil.JetExpressionWrapper) return block.getStatements()
    return block.getStatements().filter { filter!!(it) }
}

fun StatementFilter.getLastStatementInABlock(block: JetBlockExpression) = filterStatements(block).lastOrNull()