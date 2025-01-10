/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrLeafVisitor
import org.jetbrains.kotlin.ir.visitors.IrTransformer

/**
 * This node is intended to unify different ways of handling function reference-like objects in IR.
 *
 * In particular, it covers:
 * * Lambdas and anonymous functions
 * * Regular function references (`::foo`, and `receiver::foo` in code)
 * * Adapted function references, which happen in cases where referenced function doesn't perfectly match the expected shape, such as:
 *    * Returns something instead of expected `Unit`
 *    * Declares more parameters than expected, but those extra parameters have default values
 *    * Consumes vararg instead of an expected fixed number of arguments
 *    * Is not suspend, while suspend function is expected
 *    * Is a reference to a `fun interface` / SAM interface constructor, which is not a real function at all
 * * SAM or `fun interface` conversions of something listed above. E.g. `Callable { 123 }` or `Callable(::foo)`
 *
 * This node is intended to replace [IrFunctionReference] and [IrFunctionExpression] in the IR tree.
 * It also replaces some adapted function references implemented as [IrBlock] with [IrFunction] and [IrFunctionReference] inside it.
 *
 * Such objects are eventually transformed to anonymous classes, which implement the corresponding interface.
 * For example:
 *
 * ```
 * fun String.test(x: Int) : Unit = TODO()
 *
 * fun interface Foo {
 *    fun bar(x: String, y: Int): Unit
 * }
 *
 * fun main() {
 *     val x = "OK"::test
 *     //
 *     // val x = { // BLOCK
 *     //   class <anonymous>(val boundValue: String) : KFunction1<Int, Unit> {
 *     //     override fun invoke(p0: Int) { invokeFunction(boundValue, p0) }
 *     //     private static fun invokeFunction(p0: String, p1: Int) = p0.test(p1)
 *     //     // reflection information
 *     //   }
 *     //   <anonymous>("OK")
 *     // }
 *
 *     val y = Foo(String::test)
 *     // val y = { // BLOCK
 *     //   class <anonymous>() : Foo {
 *     //     override fun bar(x: String, y: Int) { invokeFunction(x, y) }
 *     //     private static fun invokeFunction(p0: String, p1: Int) = p0.test(p1)
 *     //     // reflection information
 *     //   }
 *     //   <anonymous>()
 *     // }
 * }
 * ```
 *
 * In general case, the mental model of this node is the following instance of local anonymous class:
 * ```
 * class <anonymous>(
 *     private val boundValue0: %boundValue0Type%,
 *     private val boundValue1: %boundValue1Type%,
 *     ...
 * ): %ExpressionType% {
 *     // moved from [invokeFunction] property
 *     // may be inlined to overriddenFunctionName as optimization
 *     private static fun invokeFunction(...) : %ReturnType% {
 *        // some way of invoke [reflectionTargetSymbol] or body of original lambda
 *        // it can be transformed by lowerings and plugins as other function bodies,
 *        // so no assumptions should be made on specific content of it
 *     }
 *
 *     // overriding function [overriddenFunctionSymbol]
 *     // would be created later, when node would be transformed to a class
 *     // it can't be referenced explicitly, all calls would happen with function from super-interface
 *     override fun %overriddenFunctionName%(
 *         overriddenFunctionParameter0: %overriddenFunctionParametersType0%,
 *         overriddenFunctionParameter1: %overriddenFunctionParametersType1%,
 *         ...
 *     ) = invokeFunction(
 *         boundValue0, boundValue1, ..., boundValueN,
 *         overriddenFunctionParameter0, overriddenFunctionParameter1, ..., overriddenFunctionParameterN
 *     )
 *
 *     // if reflectionTarget is not null
 *     //    some platform-specific implementation of reflection information for reflectionTarget
 *     //    some platform-specific implementation of equality/hashCode based on reflectionTarget
 * }
 * val theReference = <anonymous>(boundValues[0], boundValues[1], ..., boundValues[N])
 * ```
 *
 * So basically, this is an anonymous object implementing expression type, capturing `boundValues`, and overriding the function stored in
 * [overriddenFunctionSymbol] by the function stored in [invokeFunction], with reflection information for [reflectionTargetSymbol]
 * if it is not null.
 *
 * [invokeFunction] parameters except first [boundValues.size] correspond to non-dispatch parameters of [overriddenFunctionSymbol]
 * in natural order (i.e., contexts, extension, regular). The mapping between [invokeFunction] and [reflectionTargetSymbol] parameters
 * is not specified, and shouldn't be used. Instead, a body inside [invokeFunction] should be processed as regular expressions.
 * [boundValues] would be computed on reference creation, and then loaded from the reference object on invocation.
 *
 * [overriddenFunctionSymbol] is typically the corresponding `invoke` method of the `(K)(Suspend)FunctionN` interface, but it also can be
 * the method of a fun interface or Java SAM interface, if the corresponding SAM conversion has happened.
 *
 * [reflectionTargetSymbol] is typically a function for which the reference was initially created, or null if it is a lambda, which doesn't
 * need any reflection information.
 *
 * [hasUnitConversion], [hasSuspendConversion], [hasVarargConversion], [isRestrictedSuspension] flags represent information about
 * the reference, which is useful for generating correct reflection information. While it's technically possible to reconstruct it from
 * the function and reflection function signature, it's easier and more robust to store it explicitly.
 *
 * This allows processing function references by almost all lowerings as normal calls (within [invokeFunction]), and minimizes special
 * cases. Also, it enables support of several bound values.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.richFunctionReference]
 */
abstract class IrRichFunctionReference : IrExpression() {
    abstract var reflectionTargetSymbol: IrFunctionSymbol?

    abstract var overriddenFunctionSymbol: IrSimpleFunctionSymbol

    abstract val boundValues: MutableList<IrExpression>

    abstract var invokeFunction: IrSimpleFunction

    abstract var origin: IrStatementOrigin?

    abstract var hasUnitConversion: Boolean

    abstract var hasSuspendConversion: Boolean

    abstract var hasVarargConversion: Boolean

    abstract var isRestrictedSuspension: Boolean

    override fun <R, D> accept(visitor: IrLeafVisitor<R, D>, data: D): R =
        visitor.visitRichFunctionReference(this, data)

    override fun <D> acceptChildren(visitor: IrLeafVisitor<Unit, D>, data: D) {
        boundValues.forEach { it.accept(visitor, data) }
        invokeFunction.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        boundValues.transformInPlace(transformer, data)
        invokeFunction = invokeFunction.transform(transformer, data) as IrSimpleFunction
    }
}
