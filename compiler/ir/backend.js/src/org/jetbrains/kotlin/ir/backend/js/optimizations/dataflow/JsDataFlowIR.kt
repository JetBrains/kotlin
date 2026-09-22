/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.isExported
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * An identity-based data-flow graph for a fully linked JS program.
 *
 * A [Node] represents either a runtime value or an operation that consumes runtime values.
 * References from a consumer to its inputs are the graph's edges; there is no separate edge
 * type. Nodes are grouped into [Node.Scope]s to preserve loop nesting, but the graph does not
 * otherwise model control flow.
 *
 * Each executable body is represented by an [ExecutionUnit]. A [Function] combines the
 * identity and signature in [FunctionSymbol] with its analyzed [Body], while a
 * [FieldInitializer] represents evaluation of a static field initializer without pretending
 * that the initializer is callable.
 *
 * Builders must account for every flow of a tracked value. When an operation cannot be modeled
 * precisely, its inputs flow to [Node.OpaqueValue] and therefore escape the graph. Consumers may
 * only conclude that a value is unobserved when it reaches neither a modeled use nor such an
 * escape.
 */
object JsDataFlowIR {

    /** Coarse runtime type information used to distinguish values during analysis. */
    sealed interface Type {
        /** A JS `dynamic` value or a value whose Kotlin class is unknown or erased. */
        object Dynamic : Type

        /** A value tracked under the Kotlin class identified by [irClass]. */
        class ClassType(val irClass: IrClass) : Type
    }

    /**
     * The interned identity and signature of an [IrSimpleFunction] or [IrConstructor].
     *
     * A symbol exists independently of whether the callable has an analyzed body, allowing call
     * nodes to refer to external and bodiless declarations. For a callable with a body, [Function]
     * contains the corresponding execution unit. Constructor symbols describe constructor
     * invocation; the object produced by a constructor call is represented by [Node.NewInstance].
     *
     * [parameters] follows the IR parameter order used by call-node argument lists.
     */
    class FunctionSymbol(
        val irFunction: IrFunction,
        val isExported: Boolean,
        val parameters: Array<Type>,
        val returnType: Type,
    ) {
        init {
            require(irFunction is IrSimpleFunction || irFunction is IrConstructor) {
                "Unexpected declaration: ${irFunction.render()}"
            }
        }

        val irSimpleFunction: IrSimpleFunction? get() = irFunction as? IrSimpleFunction

        val isExternal: Boolean get() = irFunction.isEffectivelyExternal()
    }

    /**
     * The interned identity of an IR field.
     *
     * Reads, writes, and a static [FieldInitializer] share this instance, allowing consumers to
     * connect them without relying on the possibly non-unique [name].
     */
    class Field(val type: Type, val name: String?)

    /** A value, value-producing operation, or value-consuming effect in an execution unit. */
    sealed interface Node {
        object Null : Node

        /** The value received in the parameter slot at [index]. */
        class Parameter(val index: Int) : Node

        /** A value whose construction has no input flows relevant to the analysis. */
        class Const(val type: Type) : Node

        /**
         * The result of invoking [callee] with [arguments].
         *
         * Arguments are index-aligned with [FunctionSymbol.parameters]. An omitted IR argument is
         * represented by an inert placeholder instead of shifting subsequent argument indices.
         */
        sealed class Call(
            val callee: FunctionSymbol,
            val arguments: List<Node>,
            val returnType: Type,
        ) : Node

        /** A call whose runtime target is exactly [Call.callee]. */
        open class StaticCall(
            callee: FunctionSymbol,
            arguments: List<Node>,
            returnType: Type,
        ) : Call(callee, arguments, returnType)

        /** A `constructCallableReference` call that decorates a raw function with K-callable metadata. */
        class CallableReferenceWrapper(
            callee: FunctionSymbol,
            arguments: List<Node>,
            returnType: Type,
            val irCall: IrCall,
        ) : StaticCall(callee, arguments, returnType)

        /**
         * An overridable or interface call whose runtime target may be an override of [Call.callee].
         * The dispatch receiver occupies the corresponding parameter slot in [Call.arguments].
         */
        class VirtualCall(
            callee: FunctionSymbol,
            arguments: List<Node>,
            returnType: Type,
        ) : Call(callee, arguments, returnType)

        /** The existing instance returned by `IrGetObjectValue`; this node does not allocate it. */
        class Singleton(val type: Type) : Node

        /**
         * The object allocated by a constructor call.
         *
         * [constructorArguments] are index-aligned with [constructor]'s parameters and flow into
         * the constructor body. The constructor's own [Function.returns] does not represent this
         * allocated object.
         */
        class NewInstance(
            val type: Type,
            val constructor: FunctionSymbol,
            val constructorArguments: List<Node> = emptyList(),
        ) : Node

        /**
         * An explicitly modeled IR type operation, such as a type check or SAM conversion.
         *
         * [resultIsArgument] is true when the operation preserves the identity of [argument], so
         * value flow can continue through this node. Pure casts do not produce a [TypeCheck]: the
         * builder maps their result directly to their argument because the graph tracks runtime
         * values rather than changes in static type.
         */
        class TypeCheck(val argument: Node, val operator: IrTypeOperator) : Node {
            val resultIsArgument: Boolean
                get() = when (operator) {
                    IrTypeOperator.REINTERPRET_CAST,
                    IrTypeOperator.IMPLICIT_NOTNULL,
                    IrTypeOperator.IMPLICIT_DYNAMIC_CAST,
                        -> true
                    else -> false
                }
        }

        /**
         * The result of a JS dynamic member access or operation, such as `obj.foo`, `obj[i]`, or
         * `obj(...)`. A null [operator] denotes member access.
         */
        class DynamicAccess(
            val receiver: Node,
            val arguments: List<Node>,
            val operator: IrDynamicOperator? = null,
        ) : Node

        /** A raw function reference, indicating that [symbol] is used as a value. */
        class FunctionReference(val symbol: FunctionSymbol, val type: Type) : Node

        /** The value loaded from [field]; a null [receiver] denotes a static field. */
        class FieldRead(val receiver: Node?, val field: Field, val type: Type) : Node

        /** A write that consumes [value]; a null [receiver] denotes a static field. */
        class FieldWrite(val receiver: Node?, val field: Field, val value: Node) : Node

        /**
         * An escape sink for value flows the graph cannot model precisely.
         *
         * Typical inputs include values captured or reassigned by nested declarations and
         * operands of unrecognized IR. Consumers must assume that every node in [values] can be
         * observed in any way after reaching this sink.
         */
        class OpaqueValue(val values: List<Node>) : Node

        /** A merge of all [values] that may reach the same IR local or expression result. */
        class Variable(values: List<Node>, val type: Type) : Node {
            val values = mutableListOf<Node>().also { it += values }
        }

        /**
         * A graph region used to preserve loop nesting.
         *
         * A scope may contain child scopes in [nodes], forming a tree rooted at [Body.rootScope].
         * [depth] records its position in that tree but does not encode additional control flow.
         */
        class Scope(val depth: Int) : Node {
            val nodes = mutableSetOf<Node>()
        }
    }

    /**
     * The graph shared by a function or static field initializer execution unit.
     *
     * [rootScope] contains nodes outside loops. [allScopes] contains the root and every nested
     * loop scope, providing a flat traversal view of the same scope tree. [throws] merges every
     * value explicitly thrown by the body. Function returns are intentionally stored on
     * [Function], because a [FieldInitializer] is not callable and has no return flow.
     */
    class Body(
        val rootScope: Node.Scope,
        val allScopes: List<Node.Scope>,
        val throws: Node.Variable,
    ) {
        /** Visits every operation and value node, excluding the scopes that contain them. */
        inline fun forEachNonScopeNode(block: (Node) -> Unit) {
            for (scope in allScopes) {
                for (node in scope.nodes) {
                    if (node !is Node.Scope) {
                        block(node)
                    }
                }
            }
        }
    }

    /** An independently executed function body or static field initializer. */
    sealed interface ExecutionUnit {
        val body: Body
    }

    /**
     * The analyzed body of [symbol].
     *
     * [returns] merges values returned to callers. For constructors, the allocated instance is
     * represented at each call site by [Node.NewInstance], not by this merge.
     */
    class Function(
        val symbol: FunctionSymbol,
        val returns: Node.Variable,
        override val body: Body,
    ) : ExecutionUnit

    /**
     * One evaluation of a static field initializer.
     *
     * [value] is the initializer expression's result.
     * The same node is consumed by a [Node.FieldWrite] in [body], making the initialization flow visible to field
     * readers.
     */
    class FieldInitializer(
        val field: Field,
        val value: Node,
        override val body: Body,
    ) : ExecutionUnit

    /**
     * Interns the classes, functions, constructors, and fields referenced by one linked program.
     */
    class SymbolTable(val context: JsIrBackendContext) {
        val classMap = mutableMapOf<IrClass, Type.ClassType>()
        val functionMap = mutableMapOf<IrFunction, FunctionSymbol>()
        val fieldMap = mutableMapOf<IrField, Field>()

        private var sealed = false

        /** Pre-registers bodied callables and declared classes from [irModule]. */
        fun populateWith(irModule: IrModuleFragment) {
            check(!sealed) { "The symbol table is already sealed" }

            irModule.accept(object : IrVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                    declaration.body?.let { mapFunction(declaration) }
                }

                override fun visitConstructor(declaration: IrConstructor) {
                    declaration.body?.let { mapFunction(declaration) }
                }

                override fun visitClass(declaration: IrClass) {
                    declaration.acceptChildrenVoid(this)
                    mapClassReferenceType(declaration)
                }
            }, data = null)
        }

        /**
         * Forbids new mappings after all execution units have been built.
         *
         * Subsequent lookups must resolve to existing identities; an unexpected declaration then
         * fails immediately instead of silently diverging from consumers of the graph.
         */
        fun seal() {
            sealed = true
        }

        fun mapField(field: IrField): Field = fieldMap.getOrPut(field) {
            check(!sealed) { "Unmapped field in a sealed symbol table: ${field.render()}" }
            Field(type = mapType(field.type), name = field.name.asString())
        }

        fun mapClassReferenceType(irClass: IrClass): Type.ClassType {
            classMap[irClass]?.let { return it }
            check(!sealed) { "Unmapped class in a sealed symbol table: ${irClass.render()}" }
            val type = Type.ClassType(irClass)
            classMap[irClass] = type
            return type
        }

        fun mapType(type: IrType): Type {
            val inlinedClass = context.inlineClassesUtils.getInlinedClass(type)
            if (inlinedClass != null) {
                return mapClassReferenceType(inlinedClass)
            }
            if (type is IrDynamicType) {
                return Type.Dynamic
            }
            val clazz = when ((type as? IrSimpleType)?.classifier) {
                is IrClassSymbol -> type.getClass()
                is IrTypeParameterSymbol -> type.erasedUpperBound
                else -> null
            }
            return if (clazz != null) mapClassReferenceType(clazz) else Type.Dynamic
        }

        fun mapFunction(irFunction: IrFunction): FunctionSymbol {
            val actualFunction = when (irFunction) {
                is IrSimpleFunction -> irFunction.resolveDfgTarget()
                is IrConstructor -> irFunction
            }
            functionMap[actualFunction]?.let { return it }
            check(!sealed) {
                "Unmapped function in a sealed symbol table: ${actualFunction.render()}"
            }
            val symbol = FunctionSymbol(
                irFunction = actualFunction,
                isExported = actualFunction.isExported(context),
                parameters = actualFunction.parameters.map { parameter -> mapType(parameter.type) }.toTypedArray(),
                returnType = mapType(actualFunction.returnType),
            )
            functionMap[actualFunction] = symbol
            return symbol
        }
    }
}

/**
 * Resolves the callable identity used by the DFG without requiring a concrete override target.
 *
 * A declaration with a body is its own target, including the bodied `$suspendBridge` fake
 * overrides produced by
 * [org.jetbrains.kotlin.ir.backend.js.lower.coroutines.PrepareSuspendFunctionsForExportLowering].
 * A bodiless fake override resolves to its effective implementation when possible and otherwise
 * remains mapped as a bodiless callee.
 */
internal fun IrSimpleFunction.resolveDfgTarget(): IrSimpleFunction {
    if (body != null) {
        return this
    }
    val isUnresolvedFakeOverride = { it: IrSimpleFunction -> it.isFakeOverride && it.body == null }
    return resolveFakeOverride(isUnresolvedFakeOverride)
        ?: resolveFakeOverrideMaybeAbstract(isUnresolvedFakeOverride)
        ?: this
}
