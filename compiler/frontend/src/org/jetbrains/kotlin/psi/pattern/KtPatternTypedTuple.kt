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
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.types.expressions.*

class KtPatternTypedTuple(node: ASTNode) : KtPatternEntry(node) {

    val typeReference: KtTypeReference?
        get() = findChildByType(KtNodeTypes.TYPE_REFERENCE)

    val tuple: KtPatternTuple?
        get() = findChildByType(KtNodeTypes.PATTERN_TUPLE)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitPatternTypedTuple(this, data)
    }

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState): NotNullKotlinTypeInfo = resolver.restoreOrCreate(this, state) {
        typeReference?.let { typeReference ->
            resolver.getTypeInfo(typeReference, state).also {
                it.type.errorIfNull(typeReference, state, Errors.UNSPECIFIED_TYPE)
            }
        }
    }

    override fun resolve(resolver: PatternResolver, state: PatternResolveState): NotNullKotlinTypeInfo {
        val info = resolver.resolveType(this, state)
        val expressions = tuple?.expressions ?: return info
        val receiverType = resolver.getComponentsTypeInfoReceiver(this, state.replaceType(info.type)) ?: info.type
        val typesInfo = resolver.getComponentsTypeInfo(this, state.replaceType(receiverType), expressions.size)
        return info.and(typesInfo.zip(expressions) { typeInfo, expression ->
            expression.resolve(resolver, state.next(typeInfo.type))
        })
    }
}