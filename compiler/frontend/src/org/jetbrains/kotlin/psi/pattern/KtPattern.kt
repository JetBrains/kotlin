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
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.types.expressions.ConditionalTypeInfo
import org.jetbrains.kotlin.types.expressions.PatternResolveState
import org.jetbrains.kotlin.types.expressions.PatternResolver
import org.jetbrains.kotlin.types.expressions.errorAndReplaceIfNull

class KtPattern(node: ASTNode) : KtPatternElementImpl(node) {

    val innerNotPatternExpressions: List<KtExpression>
        get() = collectDescendantsOfType({ it is KtPatternElement }, { it !is KtPatternElement })

    val innerVariableDeclarations: List<KtPatternVariableDeclaration>
        get() = collectDescendantsOfType({ it is KtPatternElement }, { true })

    val entry: KtPatternEntry?
        get() = findChildByType(KtNodeTypes.PATTERN_ENTRY)

    val guard: KtPatternGuard?
        get() = findChildByType(KtNodeTypes.PATTERN_GUARD)

    val isSimple: Boolean
        get() = entry?.isSimple?.and(guard == null) ?: true

    val isRestrictionsFree: Boolean
        get() = entry?.isRestrictionsFree?.and(guard == null) ?: true

    val onlyTypeRestrictions: Boolean
        get() = entry?.onlyTypeRestrictions?.and(guard == null) ?: true

    val typeReference: KtTypeReference?
        get() = entry?.typeReference

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitPattern(this, data)
    }

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        val error = Errors.EXPECTED_PATTERN_ENTRY
        val patch = ConditionalTypeInfo.empty(state.subject.type, state.dataFlowInfo)
        val entryInfo = entry?.getTypeInfo(resolver, state).errorAndReplaceIfNull(this, state, error, patch)
        val guardState = state.replaceDataFlow(entryInfo.dataFlowInfo.thenInfo)
        val guardInfo = guard?.getTypeInfo(resolver, guardState)
        entryInfo.and(guardInfo)
    }
}