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

package org.jetbrains.kotlin.types.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.pattern.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.smartcasts.ConditionalDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.util.isSingleUnderscore
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope
import org.jetbrains.kotlin.resolve.scopes.TraceBasedLocalRedeclarationChecker
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType

class ConditionalTypeInfo(val type: KotlinType, val dataFlowInfo: ConditionalDataFlowInfo) {
    fun and(other: ConditionalTypeInfo?) = other?.let {
        ConditionalTypeInfo(type, dataFlowInfo.and(it.dataFlowInfo))
    } ?: this

    fun replaceThenInfo(thenInfo: DataFlowInfo) = ConditionalTypeInfo(type, dataFlowInfo.replaceThenInfo(thenInfo))

    companion object {
        fun empty(type: KotlinType, thenInfo: DataFlowInfo) =
                ConditionalTypeInfo(type, ConditionalDataFlowInfo(thenInfo, DataFlowInfo.EMPTY))
    }
}

class PatternResolver(
        private val patternMatchingTypingVisitor: PatternMatchingTypingVisitor,
        private val components: ExpressionTypingComponents,
        private val facade: ExpressionTypingInternals
) {
    val builtIns = components.builtIns!!

    companion object {
        fun getDeconstructName() = Name.identifier("deconstruct")
        fun getComponentName(index: Int) = DataClassDescriptorResolver.createComponentName(index + 1)
    }

    fun resolve(context: ExpressionTypingContext, pattern: KtPattern, subject: Subject, allowDefinition: Boolean, isNegated: Boolean) = run {
        val redeclarationChecker = TraceBasedLocalRedeclarationChecker(context.trace, components.overloadChecker)
        val outerScope = context.scope
        val scopeDescriptor = outerScope.ownerDescriptor
        val isOwnerDescriptorAccessibleByLabel = outerScope.isOwnerDescriptorAccessibleByLabel
        val scopeKind = outerScope.kind
        val scope = LexicalWritableScope(outerScope, scopeDescriptor, isOwnerDescriptorAccessibleByLabel, redeclarationChecker, scopeKind)
        val state = PatternResolveState(scope, context, allowDefinition, isNegated, false, subject)
        val typeInfo = pattern.getTypeInfo(this, state)
        typeInfo to scope
    }

    fun getDeconstructType(typedTuple: KtPatternTypedTuple, state: PatternResolveState): KotlinType? {
        val results = repairAfterInvoke(state) {
            components.fakeCallResolver.resolveFakeCall(
                    context = state.context,
                    receiver = state.subject.receiverValue,
                    name = getDeconstructName(),
                    callElement = state.subject.expression,
                    reportErrorsOn = typedTuple,
                    callKind = FakeCallKind.OTHER,
                    valueArguments = emptyList()
            )
        }
        if (!results.isSuccess) return null
        state.context.trace.record(BindingContext.PATTERN_DECONSTRUCT_RESOLVED_CALL, typedTuple, results.resultingCall)
        return results.resultingDescriptor.returnType
    }

    fun getComponentType(index: Int, entry: KtPatternEntry, state: PatternResolveState): KotlinType {
        val componentName = getComponentName(index)
        val results = repairAfterInvoke(state) {
            components.fakeCallResolver.resolveFakeCall(
                    context = state.context,
                    receiver = state.subject.receiverValue,
                    name = componentName,
                    callElement = state.subject.expression,
                    reportErrorsOn = entry,
                    callKind = FakeCallKind.COMPONENT,
                    valueArguments = emptyList()
            )
        }
        val errorType = lazy { ErrorUtils.createErrorType("$componentName() return type") }
        if (!results.isSuccess) return errorType.value
        val resultType = results.resultingDescriptor.returnType ?: return errorType.value
        state.context.trace.record(BindingContext.PATTERN_COMPONENT_RESOLVED_CALL, entry, results.resultingCall)
        return resultType
    }

    private fun <T> repairAfterInvoke(state: PatternResolveState, invokable: () -> T): T {
        val beforeSubjectType = state.context.trace.bindingContext.getType(state.subject.expression)
        val result = invokable()
        val subjectTypeInfo = state.context.trace.get(BindingContext.EXPRESSION_TYPE_INFO, state.subject.expression)
        if (subjectTypeInfo != null && beforeSubjectType != null) {
            val repairedSubjectTypeInfo = subjectTypeInfo.replaceType(beforeSubjectType)
            state.context.trace.record(BindingContext.EXPRESSION_TYPE_INFO, state.subject.expression, repairedSubjectTypeInfo)
        }
        return result
    }

    fun getTypeInfo(typeReference: KtTypeReference, state: PatternResolveState): ConditionalTypeInfo {
        val context = state.context
        val subjectType = state.subject.type
        val dataFlowValue = state.subject.dataFlowValue
        val isNegated = state.isNegated
        val isTuple = state.isTuple
        val (type, conditionInfo) = patternMatchingTypingVisitor.checkTypeForIs(
                context, typeReference, isNegated, subjectType, typeReference, !isTuple, dataFlowValue)
        return ConditionalTypeInfo(type, conditionInfo)
    }

    fun getTypeInfo(expression: KtExpression, state: PatternResolveState): ConditionalTypeInfo {
        val subjectType = state.context.trace.bindingContext.get(BindingContext.PATTERN_SUBJECT_TYPE, expression)
        if (subjectType == null) {
            state.context.trace.record(BindingContext.PATTERN_SUBJECT_TYPE, expression, state.subject.type)
        }
        val info = facade.getTypeInfo(expression, state.context)
        val type = info.type ?: ErrorUtils.createErrorType("${expression.text} return type")
        return ConditionalTypeInfo(type, ConditionalDataFlowInfo(info.dataFlowInfo))
    }

    fun restoreOrCreate(element: KtPatternElement, state: PatternResolveState, creator: () -> ConditionalTypeInfo): ConditionalTypeInfo {
        state.context.trace.bindingContext.get(BindingContext.PATTERN_ELEMENT_TYPE_INFO, element)?.let { return it }
        val info = creator()
        state.context.trace.record(BindingContext.PATTERN_ELEMENT_TYPE_INFO, element, info)
        return info
    }

    fun checkCondition(expression: KtExpression?, state: PatternResolveState): ConditionalDataFlowInfo {
        val context = state.context.replaceScope(state.scope)
        val visitor = ControlStructureTypingVisitor(facade)
        val conditionalInfo = visitor.checkCondition(expression, context)
        return ConditionalDataFlowInfo(conditionalInfo, context.dataFlowInfo)
    }

    fun defineVariable(declaration: KtPatternVariableDeclaration, state: PatternResolveState): DataFlowInfo {
        if (declaration.isSingleUnderscore) return state.dataFlowInfo
        val trace = state.context.trace
        if (!state.allowDefinition) {
            trace.report(Errors.NOT_ALLOW_PROPERTY_DEFINITION.on(declaration, declaration))
            return state.dataFlowInfo
        }
        val scope = state.scope
        val type = state.subject.type
        val descriptor = components.localVariableResolver.resolveLocalVariableDescriptorWithType(scope, declaration, type, trace)
        ExpressionTypingUtils.checkVariableShadowing(scope, trace, descriptor)
        scope.addVariableDescriptor(descriptor)
        val usageModuleDescriptor = DescriptorUtils.getContainingModuleOrNull(scope.ownerDescriptor)
        val variableDataFlowValue = DataFlowValueFactory.createDataFlowValue(
                declaration, descriptor, state.context.trace.bindingContext, usageModuleDescriptor)
        val dataFlowValue = state.subject.dataFlowValue
        return state.dataFlowInfo.assign(variableDataFlowValue, dataFlowValue, components.languageVersionSettings)
    }
}

fun <T, E : PsiElement> T?.errorAndReplaceIfNull(element: E, state: PatternResolveState, error: DiagnosticFactory0<E>, patch: T): T {
    if (this != null) return this
    state.context.trace.report(error.on(element))
    return patch
}

fun <T, E : PsiElement> T?.errorAndReplaceIfNull(element: E, state: PatternResolveState, error: DiagnosticFactory1<E, E>, patch: T): T {
    if (this != null) return this
    state.context.trace.report(error.on(element, element))
    return patch
}

data class Subject(val expression: KtExpression, val receiverValue: ReceiverValue, val dataFlowValue: DataFlowValue) {
    val type: KotlinType
        get() = receiverValue.type

    fun replaceType(type: KotlinType): Subject {
        val receiverValue = receiverValue.replaceType(type)
        return Subject(expression, receiverValue, dataFlowValue)
    }
}

class PatternResolveState(
        val scope: LexicalWritableScope,
        val context: ExpressionTypingContext,
        val allowDefinition: Boolean,
        val isNegated: Boolean,
        val isTuple: Boolean,
        val subject: Subject
) {
    val dataFlowInfo: DataFlowInfo
        get() = context.dataFlowInfo

    fun setIsTuple(): PatternResolveState {
        return PatternResolveState(scope, context, allowDefinition, isNegated, true, subject)
    }

    fun replaceDataFlow(dataFlowInfo: DataFlowInfo): PatternResolveState {
        val context = context.replaceDataFlowInfo(dataFlowInfo)
        return PatternResolveState(scope, context, allowDefinition, isNegated, isTuple, subject)
    }

    fun replaceSubjectType(type: KotlinType): PatternResolveState {
        val subject = subject.replaceType(type)
        return PatternResolveState(scope, context, allowDefinition, isNegated, isTuple, subject)
    }

    fun replaceSubject(subject: Subject): PatternResolveState {
        return PatternResolveState(scope, context, allowDefinition, isNegated, isTuple, subject)
    }
}
