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
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.types.expressions.ConditionalTypeInfo
import org.jetbrains.kotlin.types.expressions.PatternResolveState
import org.jetbrains.kotlin.types.expressions.PatternResolver
import org.jetbrains.kotlin.types.expressions.errorIfNull

class KtPatternTypedTuple(node: ASTNode) : KtPatternElementImpl(node) {

    val typeReference: KtPatternTypeReference?
        get() = findChildByType(KtNodeTypes.PATTERN_TYPE_REFERENCE)

    val tuple: KtPatternTuple?
        get() = findChildByType(KtNodeTypes.PATTERN_TUPLE)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitPatternTypedTuple(this, data)

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        typeReference?.getTypeInfo(resolver, state)
    }

    override fun resolve(resolver: PatternResolver, state: PatternResolveState): ConditionalTypeInfo {
        val context = state.context
        val typeReferenceInfo = typeReference?.resolve(resolver, state.setIsTuple())
        val info = resolver.resolveType(this, state).and(typeReferenceInfo)
        val entries = tuple?.entries.errorIfNull(this, state, Errors.EXPECTED_PATTERN_TUPLE_INSTANCE) ?: return info
        val receiverType = resolver.getDeconstructType(this, info.type, context) ?: info.type
        context.trace.record(BindingContext.PATTERN_COMPONENTS_RECEIVER_TYPE, this, receiverType)
        val types = resolver.getComponentsTypes(this, entries, receiverType, context)
        val componentInfo = types.zip(entries) { type, entry ->
            val subjectValue = DataFlowValueFactory.createDataFlowValue(entry, type, context)
            entry.resolve(resolver, state.replaceSubject(subjectValue, type))
        }
        return (sequenceOf(info) + componentInfo).reduce({ acc, it -> acc.and(it) })
    }
}