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
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope
import org.jetbrains.kotlin.resolve.scopes.TraceBasedLocalRedeclarationChecker
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.Printer

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

    fun resolve(context: ExpressionTypingContext, pattern: KtPattern, expectedType: KotlinType, allowDefinition: Boolean, subjectDataFlowValue: DataFlowValue): Pair<KotlinTypeInfo, LexicalScope> {
        val redeclarationChecker = TraceBasedLocalRedeclarationChecker(context.trace, components.overloadChecker)
        val scope = PatternScope(context.scope, redeclarationChecker)
        val newContext = context.replaceExpectedType(expectedType)
        val state = PatternResolveState(scope, newContext, allowDefinition, subjectDataFlowValue, redeclarationChecker)
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

    fun getComponentsTypeInfo(callElement: KtPatternTypedTuple, expressions: List<KtPatternExpression>, state: PatternResolveState) =
            TransientReceiver(state.expectedType).let { receiver ->
                expressions.mapIndexed { i, expression ->
                    getComponentTypeInfo(callElement, expression, receiver, getComponentName(i), state)
                }.map {
                    KotlinTypeInfo(it, DataFlowInfo.EMPTY)
                }
            }

    private fun getComponentTypeInfo(
            callElement: KtExpression,
            expression: KtPatternExpression,
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
        state.context.trace.record(BindingContext.PATTERN_COMPONENT_RESOLVED_CALL, expression, results.resultingCall)
        return resultType
    }

    fun getTypeInfo(typeReference: KtTypeReference, state: PatternResolveState): KotlinTypeInfo {
        val context = state.context
        val subjectType = state.expectedType
        val dataFlowValue = state.subjectDataFlowValue
        val (type, conditionInfo) = patternMatchingTypingVisitor.checkTypeForIs(context, typeReference, false, subjectType, typeReference, dataFlowValue)
        return KotlinTypeInfo(type, conditionInfo.thenInfo)
    }

    fun getTypeInfo(expression: KtExpression, state: PatternResolveState): KotlinTypeInfo {
        val info = facade.getTypeInfo(expression, state.context)
        val patch = ErrorUtils.createErrorType("${expression.text} return type")
        val type = info.type.errorAndReplaceIfNull(expression, state, Errors.NON_DERIVABLE_TYPE, patch)
        return info.replaceType(type)
    }

    fun restoreOrCreate(element: KtPatternElement, state: PatternResolveState, creator: () -> KotlinTypeInfo?): KotlinTypeInfo {
        state.context.trace.bindingContext.get(BindingContext.PATTERN_ELEMENT_TYPE_INFO, element)?.let { return it }
        val info = creator() ?: KotlinTypeInfo(state.expectedType, DataFlowInfo.EMPTY)
        state.context.trace.record(BindingContext.PATTERN_ELEMENT_TYPE_INFO, element, info)
        return info
    }

    fun resolveType(expression: KtExpression, state: PatternResolveState): KotlinTypeInfo {
        val subjectType = state.context.trace.bindingContext.get(BindingContext.PATTERN_SUBJECT_TYPE, expression)
        if (subjectType == null) {
            state.context.trace.record(BindingContext.PATTERN_SUBJECT_TYPE, expression, state.expectedType)
        }
        return getTypeInfo(expression, state)
    }

    fun resolveType(element: KtPatternElement, state: PatternResolveState): KotlinTypeInfo {
        val info = element.getTypeInfo(this, state)
        PatternMatchingTypingVisitor.checkTypeCompatibility(state.context, info.type!!, state.expectedType, element)
        return info
    }

    fun checkExpression(expression: KtExpression?, state: PatternResolveState): DataFlowInfo {
        val context = state.context.replaceScope(state.scope)
        val visitor = ControlStructureTypingVisitor(facade)
        return visitor.checkCondition(expression, context)
    }

    fun defineVariable(declaration: KtPatternVariableDeclaration, state: PatternResolveState) {
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

fun KotlinTypeInfo.and(vararg children: KotlinTypeInfo?): KotlinTypeInfo {
    return this.and(children.asSequence())
}

fun KotlinTypeInfo.and(children: Iterable<KotlinTypeInfo?>): KotlinTypeInfo {
    return this.and(children.asSequence())
}

fun KotlinTypeInfo.and(children: Sequence<KotlinTypeInfo?>): KotlinTypeInfo {
    val dataFlowInfo = children.map { it?.dataFlowInfo }
    return this.replaceDataFlowInfo(this.dataFlowInfo.and(dataFlowInfo))
}

fun DataFlowInfo.and(vararg dataFlowInfo: DataFlowInfo?): DataFlowInfo {
    return this.and(dataFlowInfo.asSequence())
}

fun DataFlowInfo.and(dataFlowInfo: Iterable<DataFlowInfo?>): DataFlowInfo {
    return this.and(dataFlowInfo.asSequence())
}

fun DataFlowInfo.and(dataFlowInfo: Sequence<DataFlowInfo?>): DataFlowInfo {
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
        val subjectDataFlowValue: DataFlowValue,
        private val redeclarationChecker: TraceBasedLocalRedeclarationChecker
) {
    val expectedType
        get() = context.expectedType

    fun replaceScope(scope: PatternScope): PatternResolveState {
        val context = context.replaceScope(scope)
        return PatternResolveState(scope, context, allowDefinition, subjectDataFlowValue, redeclarationChecker)
    }

    fun replaceType(expectedType: KotlinType?): PatternResolveState {
        val context = context.replaceExpectedType(expectedType)
        return PatternResolveState(scope, context, allowDefinition, subjectDataFlowValue, redeclarationChecker)
    }

    fun next(expectedType: KotlinType?): PatternResolveState {
        val context = context.replaceExpectedType(expectedType)
        return PatternResolveState(scope.child(redeclarationChecker), context, allowDefinition, subjectDataFlowValue, redeclarationChecker)
    }
}
