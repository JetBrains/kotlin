/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.isExported
import org.jetbrains.kotlin.ir.backend.js.utils.isJsExport
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
 * A [Function] is a [FunctionSymbol] plus a [FunctionBody]: a tree of [Node.Scope] regions
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
        class ClassType(val irClass: IrClass, val name: String) : Type {
            override fun toString(): String = name
        }
    }

    /**
     * A function in the DFG, with parameter and return types. Also, models constructors and
     * synthetic static-field initializers (an [IrField] with an initializer).
     */
    class FunctionSymbol(
        val irDeclaration: IrDeclaration,
        val name: String?,
        val isExported: Boolean,
        val parameters: Array<Type>,
        val returnType: Type,
    ) {
        init {
            require(irDeclaration is IrSimpleFunction || irDeclaration is IrConstructor || irDeclaration is IrField) {
                "Unexpected declaration: ${irDeclaration.render()}"
            }
        }

        val irSimpleFunction: IrSimpleFunction? get() = irDeclaration as? IrSimpleFunction
        val irFunction: IrFunction? get() = irDeclaration as? IrFunction

        val isExternal: Boolean get() = irDeclaration.isEffectivelyExternal()

        override fun toString(): String = name ?: irDeclaration.render()
    }

    /** A backing field, interned in the [SymbolTable] so reads and writes meet by identity. */
    class Field(val type: Type, val name: String?) {
        override fun toString() = "Field(type=$type, name=$name)"
    }

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
         * `is` / SAM / other type operators. [passthrough] means the result is the argument
         * (same object). Pure casts (`as`, implicit and safe casts) never become nodes: the
         * builder maps them to their argument, since the graph tracks objects, not static
         * types.
         */
        class TypeCheck(val argument: Node, val operator: IrTypeOperator) : Node {
            val passthrough: Boolean
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
     * Sea-of-nodes body. [rootScope] is the outermost region; [allScopes] is the scope tree
     * flattened. [returns] / [throws] are phis of every exit / throw value.
     */
    class FunctionBody(
        val rootScope: Node.Scope,
        val allScopes: List<Node.Scope>,
        val returns: Node.Variable,
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

    /** One analyzed function: identity [symbol] plus sea-of-nodes [body]. */
    class Function(val symbol: FunctionSymbol, val body: FunctionBody)

    /** Maps IR declarations to DFG types and function symbols for one linked program. */
    class SymbolTable(val context: JsIrBackendContext) {
        val classMap = mutableMapOf<IrClass, Type.ClassType>()
        val functionMap = mutableMapOf<IrDeclaration, FunctionSymbol>()
        val fieldMap = mutableMapOf<IrField, Field>()

        private var sealed = false

        fun populateWith(irModule: IrModuleFragment) {
            check(!sealed) { "The symbol table is already sealed" }

            // Ensure common built-ins are present even if absent from the user module IR.
            mapClassReferenceType(context.irBuiltIns.anyClass.owner)
            mapClassReferenceType(context.irBuiltIns.throwableClass.owner)
            mapClassReferenceType(context.irBuiltIns.unitClass.owner)
            mapClassReferenceType(context.irBuiltIns.nothingClass.owner)
            mapClassReferenceType(context.irBuiltIns.stringClass.owner)

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

                override fun visitField(declaration: IrField) {
                    if (declaration.parent !is IrFile) {
                        return
                    }
                    declaration.initializer?.let { mapFunction(declaration) }
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
            val type = Type.ClassType(irClass, name = irClass.fqNameForIrSerialization.asString())
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

        fun mapFunction(declaration: IrDeclaration): FunctionSymbol = when (declaration) {
            is IrFunction -> declaration.toSymbol()
            is IrField -> mapPropertyInitializer(declaration)
            else -> error("Unknown declaration: $declaration")
        }

        private fun IrFunction.toSymbol(): FunctionSymbol = target.let {
            functionMap[it]?.let { fn -> return fn }
            check(!sealed) { "Unmapped function in a sealed symbol table: ${it.render()}" }
            val symbol = FunctionSymbol(
                irDeclaration = it,
                name = it.computeJsDfgName(),
                isExported = it.computeIsExported(context),
                parameters = it.parameters.map { p -> mapType(p.type) }.toTypedArray(),
                returnType = mapType(it.returnType)
            )
            functionMap[it] = symbol
            return symbol
        }

        private fun mapPropertyInitializer(irField: IrField): FunctionSymbol {
            functionMap[irField]?.let { return it }
            check(!sealed) {
                "Unmapped field initializer in a sealed symbol table: ${irField.render()}"
            }
            assert(irField.isStatic) {
                "All local properties initializers should've been lowered"
            }

            val symbol = FunctionSymbol(
                irDeclaration = irField,
                name = "${irField.fqNameWhenAvailable ?: irField.name}_init",
                isExported = false,
                parameters = emptyArray(),
                returnType = mapType(context.irBuiltIns.unitType)
            )
            functionMap[irField] = symbol
            return symbol
        }
    }
}

private fun IrFunction.computeIsExported(context: JsIrBackendContext): Boolean =
    isExported(context) || isEffectivelyExternal() || isJsExport() || parentClassOrNull?.isJsExport() == true

private fun IrFunction.computeJsDfgName(): String {
    val containingDeclarationPart = parent.fqNameForIrSerialization.let {
        if (it.isRoot) "" else "$it."
    }
    val signature = nonDispatchParameters.joinToString(prefix = "(", postfix = ")") { it.type.dumpKotlinLike() }
    return "kfun:$containingDeclarationPart${name.asString()}$signature"
}
