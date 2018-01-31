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
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.expressions.*

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
        val typeReferenceInfo = typeReference?.resolve(resolver, state.setIsTuple())
        val info = getTypeInfo(resolver, state).and(typeReferenceInfo)
        val entries = tuple?.entries.errorIfNull(this, state, Errors.EXPECTED_PATTERN_TUPLE_INSTANCE) ?: return info
        val deconstructState = state.replaceSubjectType(info.type)
        val componentsState = resolver.getDeconstructType(this, deconstructState)?.let {
            val receiverValue = TransientReceiver(it)
            val dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiverValue, deconstructState.context)
            val subject = Subject(this, receiverValue, dataFlowValue)
            deconstructState.replaceSubject(subject)
        } ?: deconstructState
        componentsState.context.trace.record(BindingContext.PATTERN_COMPONENTS_RECEIVER, this, state.subject.receiverValue)
        val componentInfo = entries.mapIndexed { i, entry ->
            val type = resolver.getComponentType(i, entry, componentsState)
            val receiverValue = ExpressionReceiver.create(entry, type, componentsState.context.trace.bindingContext)
            val dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiverValue, componentsState.context)
            val subject = Subject(entry, receiverValue, dataFlowValue)
            entry.resolve(resolver, componentsState.replaceSubject(subject))
        }
        return (sequenceOf(info) + componentInfo).reduce({ acc, it -> acc.and(it) })
    }
}