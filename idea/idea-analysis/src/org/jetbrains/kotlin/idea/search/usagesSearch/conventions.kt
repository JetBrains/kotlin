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

import com.google.common.collect.ImmutableSet
import org.jetbrains.kotlin.idea.references.KtForLoopInReference
import org.jetbrains.kotlin.idea.references.KtPropertyDelegationMethodsReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DelegatedPropertyResolver
import org.jetbrains.kotlin.types.expressions.OperatorConventions.*
import org.jetbrains.kotlin.util.OperatorNameConventions

val ALL_SEARCHABLE_OPERATIONS: ImmutableSet<KtToken> = ImmutableSet
        .builder<KtToken>()
        .addAll(UNARY_OPERATION_NAMES.keys)
        .addAll(BINARY_OPERATION_NAMES.keys)
        .addAll(ASSIGNMENT_OPERATIONS.keys)
        .addAll(COMPARISON_OPERATIONS)
        .addAll(EQUALS_OPERATIONS)
        .addAll(IN_OPERATIONS)
        .add(KtTokens.LBRACKET)
        .add(KtTokens.BY_KEYWORD)
        .build()

val DELEGATE_ACCESSOR_NAMES = setOf(Name.identifier("getValue"), Name.identifier("setValue"))

val IN_OPERATIONS_TO_SEARCH = setOf(KtTokens.IN_KEYWORD)

val COMPARISON_OPERATIONS_TO_SEARCH = setOf(KtTokens.LT, KtTokens.GT)

fun Name.getOperationSymbolsToSearch(): Pair<Set<KtToken>, Class<*>>? {
    when (this) {
        OperatorNameConventions.COMPARE_TO -> return COMPARISON_OPERATIONS_TO_SEARCH to KtSimpleNameReference::class.java
        OperatorNameConventions.EQUALS -> return EQUALS_OPERATIONS to KtSimpleNameReference::class.java
        OperatorNameConventions.ITERATOR -> return IN_OPERATIONS_TO_SEARCH to KtForLoopInReference::class.java
        in DELEGATE_ACCESSOR_NAMES -> return setOf(KtTokens.BY_KEYWORD) to KtPropertyDelegationMethodsReference::class.java
        DelegatedPropertyResolver.PROPERTY_DELEGATED_FUNCTION_NAME -> return setOf(KtTokens.BY_KEYWORD) to KtPropertyDelegationMethodsReference::class.java
    }

    return null
}
