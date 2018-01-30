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
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.pattern.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.ConditionalDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.util.isSingleUnderscore
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope
import org.jetbrains.kotlin.resolve.scopes.TraceBasedLocalRedeclarationChecker
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.Printer

class ConditionalTypeInfo(val type: KotlinType, val dataFlowInfo: ConditionalDataFlowInfo) {
    fun and(other: ConditionalTypeInfo?) = other?.let {
        ConditionalTypeInfo(type, dataFlowInfo and it.dataFlowInfo)
    } ?: this
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

    fun resolve(
            context: ExpressionTypingContext,
            pattern: KtPattern,
            subjectType: KotlinType,
            allowDefinition: Boolean,
            isNegated: Boolean,
            subjectDataFlowValue: DataFlowValue
    ): Pair<ConditionalTypeInfo, LexicalScope> {
        val redeclarationChecker = TraceBasedLocalRedeclarationChecker(context.trace, components.overloadChecker)
        val scope = PatternScope(context.scope, redeclarationChecker)
        val state = PatternResolveState(scope, context, allowDefinition, isNegated, false, subjectDataFlowValue, subjectType)
        val typeInfo = pattern.resolve(this, state)
        return typeInfo to scope
    }

    fun getDeconstructType(typedTuple: KtPatternTypedTuple, receiverType: KotlinType, context: ResolutionContext<*>): KotlinType? {
        val receiver = TransientReceiver(receiverType)
        val results = components.fakeCallResolver.resolveFakeCall(
                context = context,
                receiver = receiver,
                name = getDeconstructName(),
                callElement = typedTuple,
                reportErrorsOn = typedTuple,
                callKind = FakeCallKind.OTHER,
                valueArguments = emptyList()
        )
        if (!results.isSuccess) return null
        context.trace.record(BindingContext.PATTERN_DECONSTRUCT_RESOLVED_CALL, typedTuple, results.resultingCall)
        return results.resultingDescriptor.returnType
    }

    fun getComponentsTypes(callElement: KtPatternTypedTuple, entries: List<KtPatternEntry>, receiverType: KotlinType, context: ResolutionContext<*>): List<KotlinType> {
        val receiver = TransientReceiver(receiverType)
        return entries.mapIndexed { i, entry ->
            getComponentType(callElement, entry, receiver, getComponentName(i), context)
        }
    }

    private fun getComponentType(
            callElement: KtExpression,
            entry: KtPatternEntry,
            receiver: ReceiverValue,
            componentName: Name,
            context: ResolutionContext<*>
    ): KotlinType {
        val results = components.fakeCallResolver.resolveFakeCall(
                context = context.replaceExpectedType(null),
                receiver = receiver,
                name = componentName,
                callElement = callElement,
                reportErrorsOn = callElement,
                callKind = FakeCallKind.COMPONENT,
                valueArguments = emptyList()
        )
        val errorType = lazy { ErrorUtils.createErrorType("$componentName() return type") }
        if (!results.isSuccess) return errorType.value
        val resultType = results.resultingDescriptor.returnType ?: return errorType.value
        context.trace.record(BindingContext.PATTERN_COMPONENT_RESOLVED_CALL, entry, results.resultingCall)
        return resultType
    }

    fun getTypeInfo(typeReference: KtTypeReference, state: PatternResolveState): ConditionalTypeInfo {
        val context = state.context
        val subjectType = state.subjectType
        val dataFlowValue = state.subjectValue
        val isNegated = state.isNegated
        val isTuple = state.isTuple
        val (type, conditionInfo) = patternMatchingTypingVisitor.checkTypeForIs(context, typeReference, isNegated, subjectType, typeReference, !isTuple, dataFlowValue)
        return ConditionalTypeInfo(type, conditionInfo)
    }

    fun getTypeInfo(expression: KtExpression, state: PatternResolveState): ConditionalTypeInfo {
        val info = facade.getTypeInfo(expression, state.context)
        val patch = ErrorUtils.createErrorType("${expression.text} return type")
        val type = info.type.errorAndReplaceIfNull(expression, state, Errors.NON_DERIVABLE_TYPE, patch)
        return ConditionalTypeInfo(type, ConditionalDataFlowInfo(info.dataFlowInfo))
    }

    fun restoreOrCreate(element: KtPatternElement, state: PatternResolveState, creator: () -> ConditionalTypeInfo?): ConditionalTypeInfo {
        state.context.trace.bindingContext.get(BindingContext.PATTERN_ELEMENT_TYPE_INFO, element)?.let { return it }
        val info = creator() ?: ConditionalTypeInfo(state.subjectType, ConditionalDataFlowInfo.EMPTY)
        state.context.trace.record(BindingContext.PATTERN_ELEMENT_TYPE_INFO, element, info)
        return info
    }

    fun resolveType(expression: KtExpression, state: PatternResolveState): ConditionalTypeInfo {
        val subjectType = state.context.trace.bindingContext.get(BindingContext.PATTERN_SUBJECT_TYPE, expression)
        if (subjectType == null) {
            state.context.trace.record(BindingContext.PATTERN_SUBJECT_TYPE, expression, state.subjectType)
        }
        return getTypeInfo(expression, state)
    }

    fun resolveType(element: KtPatternElement, state: PatternResolveState): ConditionalTypeInfo {
        val info = element.getTypeInfo(this, state)
        val context = state.context
        val type = info.type
        val subjectType = state.subjectType
        when (element) {
            is KtPatternTypeReference, is KtPatternVariableDeclaration, is KtPatternTypedTuple ->
                PatternMatchingTypingVisitor.checkTypeCompatibility(context, type, subjectType, element)
        }
        return info
    }

    fun checkCondition(expression: KtExpression?, state: PatternResolveState): ConditionalDataFlowInfo {
        val context = state.context.replaceScope(state.scope)
        val visitor = ControlStructureTypingVisitor(facade)
        val conditionalInfo = visitor.checkCondition(expression, context)
        return ConditionalDataFlowInfo(conditionalInfo, context.dataFlowInfo)
    }

    fun defineVariable(declaration: KtPatternVariableDeclaration, state: PatternResolveState): ConditionalTypeInfo {
        val type = declaration.getTypeInfo(this, state).type
        if (declaration.isSingleUnderscore) return ConditionalTypeInfo(type, ConditionalDataFlowInfo.EMPTY)
        val context = state.context
        val trace = context.trace
        if (!state.allowDefinition) {
            trace.report(Errors.NOT_ALLOW_PROPERTY_DEFINITION.on(declaration, declaration))
            return ConditionalTypeInfo(type, ConditionalDataFlowInfo.EMPTY)
        }
        val scope = state.scope
        val descriptor = components.localVariableResolver.resolveLocalVariableDescriptorWithType(scope, declaration, type, trace)
        ExpressionTypingUtils.checkVariableShadowing(scope, trace, descriptor)
        scope.add(descriptor)
        val variableDataFlowValue = DataFlowValueFactory.createDataFlowValue(
                declaration, descriptor, context.trace.bindingContext,
                DescriptorUtils.getContainingModuleOrNull(scope.ownerDescriptor))
        val dataFlowInfo = context.dataFlowInfo.assign(variableDataFlowValue, state.subjectValue, components.languageVersionSettings)
        return ConditionalTypeInfo(type, ConditionalDataFlowInfo(dataFlowInfo, context.dataFlowInfo))
    }
}

fun <T> T?.errorAndReplaceIfNull(
        element: KtElement,
        state: PatternResolveState,
        error: DiagnosticFactory1<KtElement, KtElement>,
        patch: T
): T = this.errorIfNull(element, state, error) ?: patch

fun <T, E : PsiElement> T?.errorIfNull(element: E, state: PatternResolveState, error: DiagnosticFactory1<E, E>) = this.also {
    it ?: state.context.trace.report(error.on(element, element))
}

class PatternScope(
        outer: LexicalScope,
        redeclarationChecker: TraceBasedLocalRedeclarationChecker
) : LexicalWritableScope(
        outer,
        outer.ownerDescriptor,
        outer.isOwnerDescriptorAccessibleByLabel,
        redeclarationChecker,
        outer.kind
) {
    fun add(descriptor: VariableDescriptor) {
        addVariableDescriptor(descriptor)
    }

    override fun printStructure(p: Printer) {
        p.println("{")
        p.pushIndent()
        addedDescriptors.forEach {
            p.println(it.toString())
        }
        p.popIndent()
        p.println("}")
    }
}

class PatternResolveState(
        val scope: PatternScope,
        val context: ExpressionTypingContext,
        val allowDefinition: Boolean,
        val isNegated: Boolean,
        val isTuple: Boolean,
        val subjectValue: DataFlowValue,
        val subjectType: KotlinType
) {
    fun setIsTuple(): PatternResolveState {
        return PatternResolveState(scope, context, allowDefinition, isNegated, true, subjectValue, subjectType)
    }

    fun replaceDataFlow(dataFlowInfo: DataFlowInfo): PatternResolveState {
        val context = context.replaceDataFlowInfo(dataFlowInfo)
        return PatternResolveState(scope, context, allowDefinition, isNegated, isTuple, subjectValue, subjectType)
    }

    fun replaceSubject(subjectValue: DataFlowValue, subjectType: KotlinType): PatternResolveState {
        return PatternResolveState(scope, context, allowDefinition, isNegated, isTuple, subjectValue, subjectType)
    }
}
