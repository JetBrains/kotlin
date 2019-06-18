/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext

sealed class ValueNode(
    val name: String,
    val type: KotlinType,
    val descriptor: DeclarationDescriptor
)

class DefaultValueNode(
    name: String,
    type: KotlinType,
    descriptor: DeclarationDescriptor
) : ValueNode(name, type, descriptor)

class ImplicitCtorValueNode(
    name: String,
    type: KotlinType,
    descriptor: DeclarationDescriptor
) : ValueNode(name, type, descriptor) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImplicitCtorValueNode

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

class AttributeNode(
    name: String,
    var isStatic: Boolean,
    val expression: KtExpression,
    type: KotlinType,
    descriptor: DeclarationDescriptor
) : ValueNode(name, type, descriptor) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttributeNode

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

enum class ValidationType {
    CHANGED,
    SET,
    UPDATE
}

class ValidatedAssignment(
    val validationType: ValidationType,
    val validationCall: ResolvedCall<*>?,
    val assignment: ResolvedCall<*>?,
    val assignmentLambda: FunctionDescriptor?,
    val attribute: AttributeNode
)

private fun getParamFnDescriptorFromCall(
    name: Name,
    call: ResolvedCall<*>?,
    context: ExpressionTypingContext
): FunctionDescriptor? {
    val param = call?.resultingDescriptor?.valueParameters?.firstOrNull {
        it.name == name
    } ?: return null
    return createFunctionDescriptor(param.type, context)
}

class ComposerCallInfo(
    context: ExpressionTypingContext,
    val composerCall: ResolvedCall<*>?,
    val pivotals: List<AttributeNode>,
    val joinKeyCall: ResolvedCall<*>?,
    val ctorCall: ResolvedCall<*>?,
    val ctorParams: List<ValueNode>,
    val validations: List<ValidatedAssignment>
) {
    val emitCtorFnDescriptor =
        getParamFnDescriptorFromCall(
            KtxNameConventions.EMIT_CTOR_PARAMETER,
            composerCall,
            context
        )
    val emitUpdaterFnDescriptor =
        getParamFnDescriptorFromCall(
            KtxNameConventions.EMIT_UPDATER_PARAMETER,
            composerCall,
            context
        )
    val emitBodyFnDescriptor =
        getParamFnDescriptorFromCall(
            KtxNameConventions.EMIT_CHILDREN_PARAMETER,
            composerCall,
            context
        )
    val callCtorFnDescriptor =
        getParamFnDescriptorFromCall(
            KtxNameConventions.CALL_CTOR_PARAMETER,
            composerCall,
            context
        )
    val callInvalidFnDescriptor =
        getParamFnDescriptorFromCall(
            KtxNameConventions.CALL_INVALID_PARAMETER,
            composerCall,
            context
        )
    val callBlockFnDescriptor =
        getParamFnDescriptorFromCall(
            KtxNameConventions.CALL_BLOCK_PARAMETER,
            composerCall,
            context
        )

    fun allAttributes(): List<ValueNode> =
        ctorParams.filter { it !is ImplicitCtorValueNode } + validations.map { it.attribute }
}

class AttributeMeta(
    val name: String,
    val type: KotlinType,
    val isChildren: Boolean,
    val descriptor: DeclarationDescriptor
)

sealed class EmitOrCallNode {
    abstract fun allAttributes(): List<ValueNode>
    abstract fun print(): String

    inline fun <T> collect(visitor: (EmitOrCallNode) -> T?): List<T> {
        val results = mutableListOf<T>()
        var node: EmitOrCallNode? = this
        while (node != null) {
            visitor(node)?.let { results.add(it) }
            node = when (node) {
                is MemoizedCallNode -> node.call
                is NonMemoizedCallNode -> node.nextCall
                is EmitCallNode -> null
                is ErrorNode -> null
            }
        }
        return results
    }

    fun resolvedCalls(): List<ResolvedCall<*>> {
        return when (this) {
            is MemoizedCallNode -> listOfNotNull(memoize.ctorCall) + call.resolvedCalls()
            is NonMemoizedCallNode ->
                listOf(resolvedCall) + (nextCall?.resolvedCalls() ?: emptyList())
            is EmitCallNode -> listOfNotNull(memoize.ctorCall)
            is ErrorNode.ResolveError -> results.allCandidates?.toList() ?: emptyList()
            is ErrorNode -> emptyList()
        }
    }

    fun allPossibleAttributes(): Map<String, List<AttributeMeta>> {
        val collector = mutableMapOf<String, MutableList<AttributeMeta>>()
        collectPossibleAttributes(collector)
        return collector
    }

    private fun collectPossibleAttributes(
        collector: MutableMap<String, MutableList<AttributeMeta>>
    ) {
        when (this) {
            is ErrorNode -> Unit
            is MemoizedCallNode -> {
                collectPossibleAttributes(
                    memoize.ctorCall?.resultingDescriptor as? FunctionDescriptor, collector
                )
                call.collectPossibleAttributes(collector)
            }
            is NonMemoizedCallNode -> {
                collectPossibleAttributes(
                    resolvedCall.resultingDescriptor as? FunctionDescriptor, collector
                )
                nextCall?.collectPossibleAttributes(collector)
            }
            is EmitCallNode -> {
                collectPossibleAttributes(
                    memoize.ctorCall?.resultingDescriptor as? FunctionDescriptor, collector
                )
            }
        }
    }

    private fun collectPossibleAttributes(
        callDescriptor: FunctionDescriptor?,
        collector: MutableMap<String, MutableList<AttributeMeta>>
    ) {
        callDescriptor?.let {
            it.valueParameters.forEach { param ->
                collector.multiPut(
                    AttributeMeta(
                        name = param.name.asString(),
                        type = param.type,
                        descriptor = param,
                        isChildren = param.hasChildrenAnnotation()
                    )
                )
            }
        }
        callDescriptor?.returnType?.let { type ->
            val cls = type.constructor.declarationDescriptor as? ClassDescriptor ?: return
            cls.unsubstitutedMemberScope.getContributedDescriptors().forEach {
                when (it) {
                    is SimpleFunctionDescriptor -> {
                        if (ComposeUtils.isSetterMethodName(
                                it.name.asString()
                            ) && it.valueParameters.size == 1) {
                            val name = ComposeUtils.propertyNameFromSetterMethod(it.name.asString())
                            collector.multiPut(
                                AttributeMeta(
                                    name = name,
                                    type = it.valueParameters.first().type,
                                    descriptor = it,
                                    isChildren = it.hasChildrenAnnotation()
                                )
                            )
                        }
                    }
                    is PropertyDescriptor -> {
                        collector.multiPut(
                            AttributeMeta(
                                name = it.name.asString(),
                                type = it.type,
                                descriptor = it,
                                isChildren = it.hasChildrenAnnotation()
                            )
                        )
                    }
                }
            }
        }
    }

    fun errorNode(): ErrorNode? {
        return when (this) {
            is ErrorNode -> this
            is MemoizedCallNode -> call.errorNode()
            is NonMemoizedCallNode -> nextCall?.errorNode()
            is EmitCallNode -> null
        }
    }
}

sealed class CallNode : EmitOrCallNode()

sealed class ErrorNode : EmitOrCallNode() {
    override fun allAttributes(): List<ValueNode> = emptyList()
    override fun print(): String = "<ERROR:${javaClass.simpleName}>"

    class NonEmittableNonCallable(val type: KotlinType) : ErrorNode()
    class RecursionLimitAmbiguousAttributesError(val attributes: Set<String>) : ErrorNode()
    class RecursionLimitError : ErrorNode()
    class NonCallableRoot : ErrorNode()
    class ResolveError(val results: OverloadResolutionResults<FunctionDescriptor>) : ErrorNode()
}

class NonMemoizedCallNode(
    val resolvedCall: ResolvedCall<*>,
    val params: List<ValueNode>,
    val postAssignments: List<ValidatedAssignment>,
    val applyCall: ResolvedCall<*>?,
    val applyLambdaDescriptor: FunctionDescriptor?,
    val applyLambdaType: KotlinType?,
    var nextCall: EmitOrCallNode?
) : CallNode() {
    override fun allAttributes(): List<ValueNode> = params
    override fun print(): String = buildString {
        self("NonMemoizedCallNode")
        attr("resolvedCall", resolvedCall) { it.print() }
        attr("params", params) { it.print() }
        list("postAssignments", postAssignments) { it.print() }
        attr("nextCall", nextCall) { it.print() }
    }
}

class MemoizedCallNode(
    val memoize: ComposerCallInfo,
    val call: EmitOrCallNode
) : CallNode() {
    override fun allAttributes(): List<ValueNode> = call.allAttributes() + memoize.allAttributes()
    override fun print(): String = buildString {
        self("MemoizedCallNode")
        attr("memoize", memoize) { it.print() }
        attr("call", call) { it.print() }
    }
}

class EmitCallNode(
    val memoize: ComposerCallInfo,
    val inlineChildren: KtExpression?
) : EmitOrCallNode() {
    override fun allAttributes(): List<ValueNode> = memoize.allAttributes()
    override fun print() = buildString {
        self("EmitCallNode")
        attr("memoize", memoize) { it.print() }
    }
}

class ResolvedKtxElementCall(
    val usedAttributes: List<AttributeNode>,
    val unusedAttributes: List<String>,
    val emitOrCall: EmitOrCallNode,
    val getComposerCall: ResolvedCall<*>?,
    val emitSimpleUpperBoundTypes: Set<KotlinType>,
    val emitCompoundUpperBoundTypes: Set<KotlinType>,
    val infixOrCall: ResolvedCall<*>?,
    val attributeInfos: Map<String, AttributeInfo>
)

fun ComposerCallInfo?.consumedAttributes(): List<AttributeNode> {
    if (this == null) return emptyList()
    return pivotals +
            ctorParams.mapNotNull { it as? AttributeNode } +
            validations.map { it.attribute }
}

fun EmitOrCallNode?.consumedAttributes(): List<AttributeNode> {
    return when (this) {
        is MemoizedCallNode -> memoize.consumedAttributes() + call.consumedAttributes()
        is NonMemoizedCallNode ->
            params.mapNotNull { it as? AttributeNode } + nextCall.consumedAttributes()
        is EmitCallNode -> memoize.consumedAttributes()
        is ErrorNode -> emptyList()
        null -> emptyList()
    }
}

val EmitOrCallNode.primaryCall: ResolvedCall<*>?
    get() = when (this) {
        is MemoizedCallNode -> memoize.ctorCall ?: call.primaryCall
        is NonMemoizedCallNode -> resolvedCall
        is EmitCallNode -> memoize.ctorCall
        is ErrorNode -> null
    }

fun List<ValueNode>.print(): String {
    return if (isEmpty()) "<empty>"
    else joinToString(", ") { it.print() }
}

fun ValueNode.print(): String = when (this) {
    is AttributeNode -> name
    is ImplicitCtorValueNode -> "(implicit)$name"
    is DefaultValueNode -> "(default)$name"
}

fun ResolvedKtxElementCall.print() = buildString {
    self("ResolvedKtxElementCall")
    attr("emitOrCall", emitOrCall) { it.print() }
    attr("usedAttributes", usedAttributes) { it.print() }
    attr("unusedAttributes", unusedAttributes) {
        it.joinToString(separator = ", ").let { s -> if (s.isBlank()) "<empty>" else s }
    }
}

fun ComposerCallInfo.print() = buildString {
    self("ComposerCallInfo")
    attr("composerCall", composerCall) { it.print() }
    attr("pivotals", pivotals) { it.print() }
    attr("joinKeyCall", joinKeyCall) { it.print() }
    attr("ctorCall", ctorCall) { it.print() }
    attr("ctorParams", ctorParams) { it.print() }
    list("validations", validations) { it.print() }
}

fun AttributeNode.print() = name
fun ResolvedCall<*>.print() = DESC_RENDERER.render(resultingDescriptor)
fun ValidatedAssignment.print() = buildString {
    self("ValidatedAssignment(${validationType.name})")
    attr("validationCall", validationCall) { it.print() }
    attr("assignment", assignment) { it.print() }
    attr("attribute", attribute) { it.print() }
}

val DESC_RENDERER = DescriptorRenderer.COMPACT_WITHOUT_SUPERTYPES.withOptions {
    parameterNamesInFunctionalTypes = false
    renderConstructorKeyword = false
    classifierNamePolicy = ClassifierNamePolicy.SHORT
    includeAdditionalModifiers = false
    unitReturnType = false
    withoutTypeParameters = true
    parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
    defaultParameterValueRenderer = null
    renderUnabbreviatedType = false
}

fun StringBuilder.self(name: String) {
    append(name)
    appendln(":")
}

fun <T> StringBuilder.attr(name: String, obj: T?, printer: (T) -> String) {
    append("  ")
    append(name)
    append(" = ")
    if (obj == null) {
        appendln("<null>")
    } else {
        appendln(printer(obj).indentExceptFirstLine("  ").trim())
    }
}

fun String.indentExceptFirstLine(indent: String = "    "): String =
    lineSequence()
        .drop(1)
        .map {
            when {
                it.isBlank() -> {
                    when {
                        it.length < indent.length -> indent
                        else -> it
                    }
                }
                else -> indent + it
            }
        }
        .joinToString("\n", prefix = lineSequence().first() + "\n")

fun <T> StringBuilder.list(name: String, obj: Collection<T>, printer: (T) -> String) {
    append("  ")
    append(name)
    if (obj.isEmpty()) {
        appendln(" = <empty>")
    } else {
        appendln(" =")
        obj.forEach {
            append("    - ")
            appendln(printer(it).indentExceptFirstLine("      ").trim())
        }
    }
}

private fun <T, V> MutableMap<T, MutableList<V>>.multiPut(key: T, value: V) {
    val current = get(key)
    if (current != null) {
        current.push(value)
    } else {
        put(key, mutableListOf(value))
    }
}

private fun MutableMap<String, MutableList<AttributeMeta>>.multiPut(value: AttributeMeta) {
    multiPut(value.name, value)
    if (value.isChildren) {
        multiPut(CHILDREN_KEY, value)
    }
}

/*

fun ResolvedCall<*>.printCode(params: List<AttributeNode>): String {
    return ""
}

fun ValidatedAssignment.printCode(): String {
    val name = attribute.name
    var result = "${validationType.name.toLowerCase()}($name)"
    when (validationType) {
        ValidationType.UPDATE -> {
            result += " { $name = it }"
        }
        ValidationType.SET -> {
            result += " { $name = it }"
        }
        ValidationType.CHANGED -> Unit
    }
    return result
}

fun ComposerCallInfo.printCode(methodName: String, itName: String, block: StringBuilder.() -> Unit) = buildString {
    append(methodName)
    append("(")
    val joinKey = (listOf("#k") + pivotals.map { it.name }).joinToString(
        separator = ",",
        prefix = "jk(",
        postfix = ")"
    )
    val ctor = if (ctorCall == null) "null"
    else "{ ${ctorCall.printCode(ctorParams)} }"

    val invalid = validations.joinToString(
        separator = " + ",
        prefix = "{ ",
        postfix = " }",
        transform = ValidatedAssignment::printCode
    )

    appendln("call($joinKey, $ctor, $invalid) { $itName ->")
    val sb = StringBuilder()
    sb.block()
    appendln(sb.toString().prependIndent("  ").trim())
    appendln("}")

}

fun ResolvedKtxElementCall.printCode() = buildString {
    when (emitOrCall) {
        is MemoizedCallNode -> {
            append(emitOrCall.memoize?.printCode("call", "f") {
                append("f()")
            })
        }
        is NonMemoizedCallNode -> {

        }
        is EmitCallNode -> {

        }
    }
}

*/
