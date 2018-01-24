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
import org.jetbrains.kotlin.resolve.calls.smartcasts.ConditionalDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope
import org.jetbrains.kotlin.resolve.scopes.TraceBasedLocalRedeclarationChecker
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.Printer

class ConditionalTypeInfo(
        val type: KotlinType,
        val dataFlowInfo: ConditionalDataFlowInfo
) {

    fun replaceType(type: KotlinType) = ConditionalTypeInfo(type, dataFlowInfo)

    fun replaceDataFlowInfo(dataFlowInfo: ConditionalDataFlowInfo) = ConditionalTypeInfo(type, dataFlowInfo)
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
            expectedType: KotlinType,
            allowDefinition: Boolean,
            isNegated: Boolean,
            subjectDataFlowValue: DataFlowValue
    ): Pair<ConditionalTypeInfo, LexicalScope> {
        val redeclarationChecker = TraceBasedLocalRedeclarationChecker(context.trace, components.overloadChecker)
        val scope = PatternScope(context.scope, redeclarationChecker)
        val newContext = context.replaceExpectedType(expectedType)
        val state = PatternResolveState(scope, newContext, allowDefinition, isNegated, false, subjectDataFlowValue, redeclarationChecker)
        val typeInfo = pattern.resolve(this, state)
        return typeInfo to scope.flatten()
    }

    fun getComponentsTypeInfoReceiver(typedTuple: KtPatternTypedTuple, state: PatternResolveState): KotlinType? {
        val receiver = TransientReceiver(state.expectedType)
        val results = components.fakeCallResolver.resolveFakeCall(
                context = state.context,
                receiver = receiver,
                name = getDeconstructName(),
                callElement = typedTuple,
                reportErrorsOn = typedTuple,
                callKind = FakeCallKind.OTHER,
                valueArguments = emptyList()
        )
        if (!results.isSuccess) return null
        state.context.trace.record(BindingContext.PATTERN_DECONSTRUCT_RESOLVED_CALL, typedTuple, results.resultingCall)
        return results.resultingDescriptor.returnType
    }

    fun getComponentsTypeInfo(callElement: KtPatternTypedTuple, entries: List<KtPatternEntry>, state: PatternResolveState) =
            TransientReceiver(state.expectedType).let { receiver ->
                entries.mapIndexed { i, entry ->
                    getComponentTypeInfo(callElement, entry, receiver, getComponentName(i), state)
                }.map {
                    ConditionalTypeInfo(it, ConditionalDataFlowInfo.EMPTY)
                }
            }

    private fun getComponentTypeInfo(
            callElement: KtExpression,
            entry: KtPatternEntry,
            receiver: ReceiverValue,
            componentName: Name,
            state: PatternResolveState
    ): KotlinType {
        val results = components.fakeCallResolver.resolveFakeCall(
                context = state.context.replaceExpectedType(null),
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
        state.context.trace.record(BindingContext.PATTERN_COMPONENT_RESOLVED_CALL, entry, results.resultingCall)
        return resultType
    }

    fun getTypeInfo(typeReference: KtTypeReference, state: PatternResolveState): ConditionalTypeInfo {
        val context = state.context
        val subjectType = state.expectedType
        val dataFlowValue = state.subjectDataFlowValue
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
        val info = creator() ?: ConditionalTypeInfo(state.expectedType, ConditionalDataFlowInfo.EMPTY)
        state.context.trace.record(BindingContext.PATTERN_ELEMENT_TYPE_INFO, element, info)
        return info
    }

    fun resolveType(expression: KtExpression, state: PatternResolveState): ConditionalTypeInfo {
        val subjectType = state.context.trace.bindingContext.get(BindingContext.PATTERN_SUBJECT_TYPE, expression)
        if (subjectType == null) {
            state.context.trace.record(BindingContext.PATTERN_SUBJECT_TYPE, expression, state.expectedType)
        }
        return getTypeInfo(expression, state)
    }

    fun resolveType(element: KtPatternElement, state: PatternResolveState): ConditionalTypeInfo {
        val info = element.getTypeInfo(this, state)
        PatternMatchingTypingVisitor.checkTypeCompatibility(state.context, info.type, state.expectedType, element)
        return info
    }

    fun checkCondition(expression: KtExpression?, state: PatternResolveState): ConditionalDataFlowInfo {
        val context = state.context.replaceScope(state.scope)
        val visitor = ControlStructureTypingVisitor(facade)
        val conditionalInfo = visitor.checkCondition(expression, context)
        return ConditionalDataFlowInfo(conditionalInfo, context.dataFlowInfo)
    }

    fun defineVariable(declaration: KtPatternVariableDeclaration, state: PatternResolveState) {
        if (declaration.isEmpty) return
        val trace = state.context.trace
        if (!state.allowDefinition) {
            trace.report(Errors.NOT_ALLOW_PROPERTY_DEFINITION.on(declaration, declaration))
            return
        }
        val scope = state.scope
        val componentType = declaration.getTypeInfo(this, state).type
        val variableDescriptor = components.localVariableResolver.resolveLocalVariableDescriptorWithType(scope, declaration, componentType, trace)
        ExpressionTypingUtils.checkVariableShadowing(scope.flatten(), trace, variableDescriptor)
        scope.add(variableDescriptor)
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

fun ConditionalTypeInfo.and(vararg children: ConditionalTypeInfo?): ConditionalTypeInfo {
    return this.and(children.asSequence())
}

fun ConditionalTypeInfo.and(children: Iterable<ConditionalTypeInfo?>): ConditionalTypeInfo {
    return this.and(children.asSequence())
}

fun ConditionalTypeInfo.and(children: Sequence<ConditionalTypeInfo?>): ConditionalTypeInfo {
    val dataFlowInfo = children.map { it?.dataFlowInfo }
    return this.replaceDataFlowInfo(this.dataFlowInfo.and(dataFlowInfo))
}

fun ConditionalDataFlowInfo.and(vararg dataFlowInfo: ConditionalDataFlowInfo?): ConditionalDataFlowInfo {
    return this.and(dataFlowInfo.asSequence())
}

fun ConditionalDataFlowInfo.and(dataFlowInfo: Iterable<ConditionalDataFlowInfo?>): ConditionalDataFlowInfo {
    return this.and(dataFlowInfo.asSequence())
}

fun ConditionalDataFlowInfo.and(dataFlowInfo: Sequence<ConditionalDataFlowInfo?>): ConditionalDataFlowInfo {
    return (sequenceOf(this) + dataFlowInfo).filterNotNull().reduce { info, it -> info.and(it) }
}

class PatternScope private constructor(
        private val outer: LexicalScope,
        private val owner: PatternScope?,
        redeclarationChecker: TraceBasedLocalRedeclarationChecker
) : LexicalWritableScope(
        outer,
        outer.ownerDescriptor,
        outer.isOwnerDescriptorAccessibleByLabel,
        redeclarationChecker,
        outer.kind
) {
    constructor(outerScope: LexicalScope, redeclarationChecker: TraceBasedLocalRedeclarationChecker) : this(outerScope, null, redeclarationChecker)
    constructor(parent: PatternScope, redeclarationChecker: TraceBasedLocalRedeclarationChecker) : this(parent.outer, parent, redeclarationChecker)

    private val children = mutableListOf<PatternScope>()

    fun add(descriptor: VariableDescriptor) {
        addVariableDescriptor(descriptor)
        owner?.add(descriptor)
    }

    fun child(redeclarationChecker: TraceBasedLocalRedeclarationChecker): PatternScope {
        val child = PatternScope(this, redeclarationChecker)
        children.add(child)
        return child
    }

    fun flatten(): LexicalScope {
        var scope = this
        while (scope.owner != null) {
            scope = scope.owner as PatternScope
        }
        return scope
    }

    override fun printStructure(p: Printer) {
        p.println("{")
        p.pushIndent()
        addedDescriptors.forEach {
            p.println(it.toString())
        }
        children.forEach { it.printStructure(p) }
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
        val subjectDataFlowValue: DataFlowValue,
        private val redeclarationChecker: TraceBasedLocalRedeclarationChecker
) {
    val expectedType
        get() = context.expectedType

    fun replaceScope(scope: PatternScope): PatternResolveState {
        val context = context.replaceScope(scope)
        return PatternResolveState(scope, context, allowDefinition, isNegated, isTuple, subjectDataFlowValue, redeclarationChecker)
    }

    fun setIsTuple(): PatternResolveState {
        return PatternResolveState(scope, context, allowDefinition, isNegated, true, subjectDataFlowValue, redeclarationChecker)
    }

    fun replaceType(expectedType: KotlinType?): PatternResolveState {
        val context = context.replaceExpectedType(expectedType)
        return PatternResolveState(scope, context, allowDefinition, isNegated, isTuple, subjectDataFlowValue, redeclarationChecker)
    }

    fun next(expectedType: KotlinType?): PatternResolveState {
        val context = context.replaceExpectedType(expectedType)
        val scope = scope.child(redeclarationChecker)
        return PatternResolveState(scope, context, allowDefinition, isNegated, isTuple, subjectDataFlowValue, redeclarationChecker)
    }
}
