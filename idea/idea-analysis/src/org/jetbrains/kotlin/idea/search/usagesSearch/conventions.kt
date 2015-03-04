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

package org.jetbrains.kotlin.idea.search.usagesSearch

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.types.expressions.OperatorConventions.*
import com.google.common.collect.ImmutableSet
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.lexer.JetSingleValueToken
import org.jetbrains.kotlin.resolve.dataClassUtils.isComponentLike
import org.jetbrains.kotlin.resolve.*

public val ALL_SEARCHABLE_OPERATIONS: ImmutableSet<JetToken> = ImmutableSet
        .builder<JetToken>()
        .addAll(UNARY_OPERATION_NAMES.keySet())
        .addAll(BINARY_OPERATION_NAMES.keySet())
        .addAll(ASSIGNMENT_OPERATIONS.keySet())
        .addAll(COMPARISON_OPERATIONS)
        .addAll(EQUALS_OPERATIONS)
        .addAll(IDENTITY_EQUALS_OPERATIONS)
        .addAll(IN_OPERATIONS)
        .add(JetTokens.LBRACKET)
        .add(JetTokens.LPAR)
        .add(JetTokens.BY_KEYWORD)
        .build()

public val ALL_SEARCHABLE_OPERATION_PATTERNS: Set<String> =
        ALL_SEARCHABLE_OPERATIONS.map { (it as JetSingleValueToken).getValue() }.toSet()

public val INDEXING_OPERATION_NAMES: ImmutableSet<Name> =
        ImmutableSet.of(Name.identifier("get"), Name.identifier("set"))

public val INVOKE_OPERATION_NAME: Name = Name.identifier("invoke")

public val ITERATOR_OPERATION_NAME: Name = Name.identifier("iterator")

public val IN_OPERATIONS_TO_SEARCH: ImmutableSet<JetToken> = ImmutableSet.of(JetTokens.IN_KEYWORD)

public val COMPARISON_OPERATIONS_TO_SEARCH: ImmutableSet<JetToken> = ImmutableSet.of<JetToken>(JetTokens.LT, JetTokens.GT)

public fun Name.getOperationSymbolsToSearch(): Set<JetToken> {
    when (this) {
        COMPARE_TO -> return COMPARISON_OPERATIONS_TO_SEARCH
        EQUALS -> return EQUALS_OPERATIONS
        IDENTITY_EQUALS -> return IDENTITY_EQUALS_OPERATIONS
        CONTAINS -> return IN_OPERATIONS_TO_SEARCH
        ITERATOR_OPERATION_NAME -> return ImmutableSet.of<JetToken>(JetTokens.IN_KEYWORD)
        in INDEXING_OPERATION_NAMES -> return ImmutableSet.of<JetToken>(JetTokens.LBRACKET, JetTokens.BY_KEYWORD)
        DelegatedPropertyResolver.PROPERTY_DELEGATED_FUNCTION_NAME -> return ImmutableSet.of<JetToken>(JetTokens.BY_KEYWORD)
    }

    if (isComponentLike(this)) return ImmutableSet.of<JetToken>(JetTokens.LPAR)

    val unaryOp = UNARY_OPERATION_NAMES.inverse()[this]
    if (unaryOp != null) return ImmutableSet.of(unaryOp)

    val binaryOp = BINARY_OPERATION_NAMES.inverse()[this]
    if (binaryOp != null) {
        val assignmentOp = ASSIGNMENT_OPERATION_COUNTERPARTS.inverse()[binaryOp]
        return if (assignmentOp != null) ImmutableSet.of<JetToken>(binaryOp, assignmentOp) else ImmutableSet.of<JetToken>(binaryOp)
    }

    val assignmentOp = ASSIGNMENT_OPERATIONS.inverse()[this]
    if (assignmentOp != null) return ImmutableSet.of<JetToken>(assignmentOp)

    return ImmutableSet.of<JetToken>()
}
