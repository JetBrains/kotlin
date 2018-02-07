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
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.expressions.*

class KtPatternTypedTuple(node: ASTNode) : KtPatternElementImpl(node) {

    val typeReference: KtPatternTypeReference?
        get() = findChildByType(KtNodeTypes.PATTERN_TYPE_REFERENCE)

    val tuple: KtPatternTuple?
        get() = findChildByType(KtNodeTypes.PATTERN_TUPLE)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitPatternTypedTuple(this, data)

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        val typeReferenceInfo = typeReference?.getTypeInfo(resolver, state.setIsTuple()) ?: ConditionalTypeInfo.empty(state.subject.type, state.dataFlowInfo)
        val deconstructState = state.replaceSubjectType(typeReferenceInfo.type)
        val componentsState = resolver.getDeconstructType(this, deconstructState)?.let {
            val receiverValue = TransientReceiver(it)
            val dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiverValue, deconstructState.context)
            val subject = Subject(this, receiverValue, dataFlowValue)
            deconstructState.replaceSubject(subject)
        } ?: deconstructState
        componentsState.context.trace.record(BindingContext.PATTERN_COMPONENTS_RECEIVER, this, deconstructState.subject.receiverValue)
        val error = Errors.EXPECTED_PATTERN_TYPED_TUPLE_INSTANCE
        val patch = ConditionalTypeInfo.empty(componentsState.subject.type, componentsState.dataFlowInfo)
        val tupleInfo = tuple?.getTypeInfo(resolver, componentsState).errorAndReplaceIfNull(this, componentsState, error, patch)
        typeReferenceInfo.and(tupleInfo)
    }
}