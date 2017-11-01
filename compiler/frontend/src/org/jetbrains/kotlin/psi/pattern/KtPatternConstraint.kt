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
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.types.expressions.NotNullKotlinTypeInfo
import org.jetbrains.kotlin.types.expressions.PatternResolveState
import org.jetbrains.kotlin.types.expressions.PatternResolver
import org.jetbrains.kotlin.types.expressions.and

class KtPatternConstraint(node: ASTNode) : KtPatternElement(node) {

    val entry: KtPatternEntry?
        get() = findChildByClass(KtPatternEntry::class.java)

    val guard: KtPatternGuard?
        get() = findChildByType(KtNodeTypes.PATTERN_GUARD)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitPatternConstraint(this, data)
    }

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        entry?.getTypeInfo(resolver, state)
    }

    override fun resolve(resolver: PatternResolver, state: PatternResolveState): NotNullKotlinTypeInfo {
        val entryInfo = entry?.resolve(resolver, state)
        val dataFlowInfo = guard?.resolve(resolver, state)
        val thisInfo = resolver.resolveType(this, state)
        val info = thisInfo.and(entryInfo)
        return info.replaceDataFlowInfo(info.dataFlowInfo.and(dataFlowInfo))
    }
}