/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi.pattern

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.types.expressions.ConditionalTypeInfo
import org.jetbrains.kotlin.types.expressions.PatternResolveState
import org.jetbrains.kotlin.types.expressions.PatternResolver
import org.jetbrains.kotlin.types.expressions.errorIfNull

class KtPatternConstraint(node: ASTNode) : KtPatternElementImpl(node) {

    val typeReference: KtPatternTypeReference?
        get() = findChildByType(KtNodeTypes.PATTERN_TYPE_REFERENCE)

    val typedTuple: KtPatternTypedTuple?
        get() = findChildByType(KtNodeTypes.PATTERN_TYPED_TUPLE)

    val expression: KtPatternExpression?
        get() = findChildByType(KtNodeTypes.PATTERN_EXPRESSION)

    val element: KtPatternElement?
        get() = findChildByClass(KtPatternElement::class.java)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitPatternConstraint(this, data)

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        val element = element.errorIfNull(this, state, Errors.EXPECTED_CONSTRAINT_ELEMENT)
        element?.getTypeInfo(resolver, state)
    }

    override fun resolve(resolver: PatternResolver, state: PatternResolveState): ConditionalTypeInfo {
        val element = element.errorIfNull(this, state, Errors.EXPECTED_CONSTRAINT_ELEMENT)
        val elementTypeInfo = element?.resolve(resolver, state)
        val thisTypeInfo = resolver.resolveType(this, state)
        return thisTypeInfo.and(elementTypeInfo)
    }
}
