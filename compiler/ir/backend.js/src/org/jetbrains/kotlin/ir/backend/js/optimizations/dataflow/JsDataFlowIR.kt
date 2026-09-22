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
 * Sea-of-nodes data-flow IR for one linked JS program.
 *
 * A [Function] is a [FunctionSymbol] plus a [Body]: a tree of [Node.Scope] regions
 * mirroring loop nesting, in which each value is a [Node] referenced directly by its consumers.
 * The [Node] subtypes document what the graph represents.
 *
 * The contract every builder and consumer relies on: each flow of a value into a consumer is
 * recorded as a use, and a flow the graph cannot model precisely is recorded as an escape via
 * [Node.OpaqueValue] — never silently dropped. Analyses that prove "nothing observes X" are
 * sound only under this rule: a lost edge means optimizing based on a use that was never seen.
 */
object JsDataFlowIR {

    /** A DFG value type: either JS [Dynamic] or a Kotlin [ClassType]. */
    sealed interface Type {
        /** JS `dynamic`, or a type with no [IrClass] (unknown / erased). */
        object Dynamic : Type

        /** Kotlin class type. Identity is [irClass]. */
        class ClassType(val irClass: IrClass) : Type
    }

    /** A function or constructor in the DFG, with parameter and return types. */
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

    /** A backing field, interned in the [SymbolTable] so reads and writes meet by identity. */
    class Field(val type: Type, val name: String?)

    sealed interface Node {
        object Null : Node

        /** Callee argument slot; [index] matches [FunctionSymbol.parameters]. */
        class Parameter(val index: Int) : Node

        /** A value with no incoming edges the analysis needs: literals, class references, lambdas. */
        class Const(val type: Type) : Node

        sealed class Call(
            val callee: FunctionSymbol,
            val arguments: List<Node>,
            val returnType: Type,
        ) : Node

        /** Direct call. */
        open class StaticCall(
            callee: FunctionSymbol,
            arguments: List<Node>,
            returnType: Type,
        ) : Call(callee, arguments, returnType)

        /**
         * The call that attaches K-callable reflection metadata to a raw function
         * (`constructCallableReference`).
         */
        class CallableReferenceWrapper(
            callee: FunctionSymbol,
            arguments: List<Node>,
            returnType: Type,
            val irCall: IrCall,
        ) : StaticCall(callee, arguments, returnType)

        /**
         * Overridable / interface dispatch; the [callee] is overridable by construction, and the
         * runtime callee may be any of its overrides. Receiver is [arguments] index 0.
         */
        class VirtualCall(
            callee: FunctionSymbol,
            arguments: List<Node>,
            returnType: Type,
        ) : Call(callee, arguments, returnType)

        /** An object (`IrGetObjectValue`) instance. */
        class Singleton(val type: Type) : Node

        /** `new` of [type]; [constructorArguments] are index-aligned with the constructor's parameters. */
        class NewInstance(
            val type: Type,
            val constructor: FunctionSymbol,
            val constructorArguments: List<Node> = emptyList(),
        ) : Node

        /**
         * `is` / SAM / other type operators. [resultIsArgument] means the result is the argument
         * (same object). Pure casts (`as`, implicit and safe casts) never become nodes: the
         * builder maps them to their argument, since the graph tracks objects, not static
         * types.
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
         * JS dynamic member or operator (`obj.foo`, `obj[i]`, `obj(...)`).
         * [operator] is set for [org.jetbrains.kotlin.ir.expressions.IrDynamicOperatorExpression];
         * `null` is member access.
         */
        class DynamicAccess(
            val receiver: Node,
            val arguments: List<Node>,
            val operator: IrDynamicOperator? = null,
        ) : Node

        /** `IrRawFunctionReference`: the function's address is taken. */
        class FunctionReference(val symbol: FunctionSymbol, val type: Type) : Node

        /** `IrGetField`. Null [receiver] is a static field. */
        class FieldRead(val receiver: Node?, val field: Field, val type: Type) : Node

        /** `IrSetField`. Null [receiver] is a static field. */
        class FieldWrite(val receiver: Node?, val field: Field, val value: Node) : Node

        /**
         * A sink the graph cannot see through: vararg elements, values captured or reassigned by
         * nested functions, children of unrecognized IR. Anything reaching [values] escapes the
         * analysis, so consumers must assume it is observed in every possible way.
         */
        class OpaqueValue(val values: List<Node>) : Node

        /** Phi of [values]: an IR local or an expression with several possible values. */
        class Variable(values: List<Node>, val type: Type) : Node {
            val values = mutableListOf<Node>().also { it += values }
        }

        /** Nested region of the sea-of-nodes; scopes form a tree mirroring loop nesting. */
        class Scope(val depth: Int) : Node {
            val nodes = mutableSetOf<Node>()
        }
    }

    /**
     * Sea-of-nodes body shared by functions and static-field initializers.
     * [rootScope] is the outermost region, [allScopes] is the flattened scope tree, and
     * [throws] is the phi of every thrown value.
     */
    class Body(
        val rootScope: Node.Scope,
        val allScopes: List<Node.Scope>,
        val throws: Node.Variable,
    ) {
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

    sealed interface ExecutionUnit {
        val body: Body
    }

    /** One analyzed function: identity [symbol] plus sea-of-nodes [body]. */
    class Function(
        val symbol: FunctionSymbol,
        val returns: Node.Variable,
        override val body: Body,
    ) : ExecutionUnit

    /** Evaluation of a static field's initializer and the value assigned to [field]. */
    class FieldInitializer(
        val field: Field,
        val value: Node,
        override val body: Body,
    ) : ExecutionUnit

    /** Maps IR declarations to DFG types and function symbols for one linked program. */
    class SymbolTable(val context: JsIrBackendContext) {
        val classMap = mutableMapOf<IrClass, Type.ClassType>()
        val functionMap = mutableMapOf<IrFunction, FunctionSymbol>()
        val fieldMap = mutableMapOf<IrField, Field>()

        private var sealed = false

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
         * Forbids interning new symbols. Called once the whole program is mapped: a later
         * per-function rebuild (after an IR-mutating pass) must resolve within the existing
         * table, so an unexpected new symbol fails loudly instead of desynchronizing consumers.
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
 * Like [target], but total.
 * [org.jetbrains.kotlin.ir.backend.js.lower.coroutines.PrepareSuspendFunctionsForExportLowering] emits bodied
 * `$suspendBridge` functions flagged as fake overrides whose only real override is abstract,
 * and [target] fails on those shapes. Here a function with a body is always its own target —
 * that body is what executes — and a bodiless fake override resolves to the member that
 * actually runs for it (possibly such a bodied bridge), falling back to the abstract
 * declaration, which is mapped as an ordinary bodiless callee.
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
