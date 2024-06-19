/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.inlineDeclaration
import org.jetbrains.kotlin.ir.util.isBuiltInSuspendCoroutine
import org.jetbrains.kotlin.ir.util.isBuiltInSuspendCoroutineUninterceptedOrReturn
import org.jetbrains.kotlin.ir.util.isFunctionInlining
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * An IR visitor that also has a customization point for handling lambdas passed to inline functions (see [visitInlineLambda]).
 */
abstract class IrInlineReferenceLocator(private val context: JvmBackendContext) : IrElementVisitor<Unit, IrDeclaration?> {
    override fun visitElement(element: IrElement, data: IrDeclaration?) =
        element.acceptChildren(this, if (element is IrDeclaration && element !is IrVariable) element else data)

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: IrDeclaration?) {
        val function = expression.symbol.owner
        if (function.isInlineFunctionCall(context)) {
            for (parameter in function.valueParameters) {
                val lambda = expression.getValueArgument(parameter.index)?.unwrapInlineLambda() ?: continue
                visitInlineLambda(lambda, function, parameter, data!!)
            }
        }
        return super.visitFunctionAccess(expression, data)
    }

    /**
     * Called by this visitor whenever a lambda is passed to an inline function.
     *
     * @param argument The lambda expression passed as an argument to [callee].
     * @param callee The inline function.
     * @param parameter The parameter of [callee] to which the lambda is passed.
     * @param scope The declaration in scope of which [callee] is being called.
     */
    abstract fun visitInlineLambda(argument: IrFunctionReference, callee: IrFunction, parameter: IrValueParameter, scope: IrDeclaration)
}

/**
 * An IR visitor that collects scopes in which inline functions/lambdas are called.
 *
 * Usage: call [findInlineCallSites] on an [IrFile], and then use [findContainer] to determine the class or package from which an inline
 * function is accessible.
 */
class IrInlineScopeResolver(context: JvmBackendContext) : IrInlineReferenceLocator(context) {

    /**
     * Describes a scope in which an inline function is called.
     *
     * @property scope [IrDeclaration] or [IrDeclarationParent] in which a function is called
     * @property approximateToPackage Whether the inline function being called can be inlined into some other class in the same package
     *   (for exmaple, if it's a crossinline lambda), thus forcing the [findContainer] to return that package instead of a class.
     */
    private class CallSite(val scope: IrElement?, val approximateToPackage: Boolean)

    /**
     * For every private inline function/lambda stores the innermost _common_ scope in which it is called.
     * For example, for function `foo` in the following code, the innermost common scope would be the class `Nested`.
     * ```kotlin
     * package com.example
     *
     * class C {
     *     private inline fun foo() = 42
     *
     *     class Nested {
     *         private inline fun bar() = C().foo()
     *         private inline fun baz() = C().foo()
     *     }
     * }
     * ```
     *
     * Populated by [findContainer].
     */
    private val inlineCallSites = mutableMapOf<IrFunction, CallSite>()

    /**
     * For each private inline function stores the set of innermost scopes in which that private inline function is called.
     */
    private val privateInlineFunctionCallSites = mutableMapOf<IrFunction, Set<IrDeclaration>>()

    override fun visitInlineLambda(argument: IrFunctionReference, callee: IrFunction, parameter: IrValueParameter, scope: IrDeclaration) {
        // suspendCoroutine and suspendCoroutineUninterceptedOrReturn accept crossinline lambdas to disallow non-local returns,
        // but these lambdas are effectively inline
        inlineCallSites[argument.symbol.owner] =
            CallSite(scope, approximateToPackage = parameter.isCrossinline && !callee.isCoroutineIntrinsic())
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: IrDeclaration?) {
        if (declaration.isPrivateInline) {
            privateInlineFunctionCallSites.putIfAbsent(declaration, mutableSetOf())
        }
        super.visitSimpleFunction(declaration, data)
    }

    override fun visitCall(expression: IrCall, data: IrDeclaration?) {
        val callee = expression.symbol.owner
        if (callee.isPrivateInline && data != null) {
            (privateInlineFunctionCallSites.getOrPut(callee) { mutableSetOf() } as MutableSet).add(data)
        }
        super.visitCall(expression, data)
    }

    override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock, data: IrDeclaration?) {
        if (inlinedBlock.isFunctionInlining()) {
            val callee = inlinedBlock.inlineDeclaration
            if (callee is IrSimpleFunction && callee.isPrivateInline && data != null) {
                (privateInlineFunctionCallSites.getOrPut(callee) { mutableSetOf() } as MutableSet).add(data)
            }
        }

        super.visitInlinedFunctionBlock(inlinedBlock, data)
    }

    private inline val IrSimpleFunction.isPrivateInline
        get() = isInline && DescriptorVisibilities.isPrivate(visibility)

    private fun IrFunction.isCoroutineIntrinsic(): Boolean =
        isBuiltInSuspendCoroutine() || isBuiltInSuspendCoroutineUninterceptedOrReturn()

    /**
     * The class from which all accesses in [scope] will be done after bytecode generation.
     * If [scope] is a crossinline lambda, this is not possible, as the lambda may be inlined
     * into some other class; in that case, return at least the package fragment.
     *
     * @param scope [IrDeclaration] or [IrDeclarationParent].
     * @return [IrClass] or at least [IrPackageFragment] from which all accesses in the current scope will be done after bytecode
     *   generation, or `null` if the space of potential call sites is unconstrained, e.g. if [scope] is referenced from an internal inline
     *   function.
     */
    fun findContainer(scope: IrElement): IrDeclarationContainer? =
        findContainer(scope, approximateToPackage = false)

    /**
     * The class from which all accesses in [scope] will be done after bytecode generation.
     * If [scope] is a crossinline lambda, this is not possible, as the lambda may be inlined
     * into some other class; in that case, return at least the package fragment.
     *
     * @param scope [IrDeclaration] or [IrDeclarationParent]
     * @param approximateToPackage Whether the inline function being called can be inlined into some other class in the same package,
     *   for example, if it's a crossinline lambda.
     * @return [IrClass] or at least [IrPackageFragment] from which all accesses in the current scope will be done after bytecode
     *   generation, or `null` if the space of potential call sites is unconstrained, e.g. if [scope] is referenced from an internal inline
     *   function.
     */
    private tailrec fun findContainer(scope: IrElement?, approximateToPackage: Boolean): IrDeclarationContainer? {
        val callSite = inlineCallSites[scope]
        return when {
            // Crossinline lambdas can be inlined into some other class in the same package. However,
            // classes within crossinline lambdas should not be regenerated, so if we've already found
            // a class *before* reaching this lambda, it's valid:
            //     class C {
            //         fun f() {}
            //         fun g() = inlineFunctionWithCrossinlineArgument {
            //             f() // this call is done in some unknown class within C's package
            //             object { val x = f() } // this call is done in C$g$1$1
            //         }
            //     }
            callSite != null -> findContainer(callSite.scope, approximateToPackage || callSite.approximateToPackage)
            // Inline functions can be inlined into anywhere. Not even private inline functions are safe:
            //     class C {
            //         fun f() {}
            //         private inline fun g1() = f() // `f` is called from C?
            //         fun g2() = { g1() } // ...or from C$g2$1 in the same package?
            //         inline fun g3() = g1() // ...or from some other package that calls g3?
            //     }
            // However, for private ones we at least know where they're called, so just like inline lambdas,
            // we can navigate there.
            //
            // TODO: this has some weird effects for inline functions in local classes, e.g. they
            //   access the capture fields (package-private) through accessors; this may or may not
            //   be necessary - local types should in theory not be usable outside the current file.
            scope is IrFunction && scope.isInline -> {
                val callSites = privateInlineFunctionCallSites[scope] ?: return null
                // Mark to avoid infinite recursion on self-recursive inline functions (those are only
                // detected reliably by codegen; frontend only filters out simple cases).
                inlineCallSites[scope] = CallSite(scope = null, approximateToPackage = false)
                val commonCallSite = when {
                    callSites.isEmpty() -> CallSite(scope.parent, approximateToPackage = false)
                    callSites.size == 1 -> CallSite(callSites.single(), approximateToPackage = false)
                    else -> {
                        @Suppress("NON_TAIL_RECURSIVE_CALL")
                        val results = callSites.map { findContainer(it, approximateToPackage = false) ?: return null }
                        // If all call sites are within a single class, use it. Otherwise, all scopes must be within
                        // the current file's package.
                        val single = results.first().takeIf { results.all { other -> it === other } }
                        CallSite(single ?: scope.parent, approximateToPackage = single == null)
                    }
                }
                inlineCallSites[scope] = commonCallSite
                findContainer(commonCallSite.scope, approximateToPackage || commonCallSite.approximateToPackage)
            }
            // TODO: if this class is an object local to an inline function, it could be regenerated,
            //  so the scope depends on the declaration accessed (see KT-48508):
            //     class C {
            //         fun f1()
            //         inline fun inlineFun() = object {
            //             fun f2() {}
            //             fun g1() {
            //                 f1() // this access can be anywhere
            //                 f2() // can pretend this access is from C$foo$1
            //             }
            //         }
            //     }
            //  Further complicating things, the accessor for `f1` cannot be in `C$inlineFun$1`, as otherwise
            //  the accessor itself will be regenerated (and thus not work) at `inlineFun` call sites.
            scope is IrClass && !approximateToPackage -> scope
            // Inline lambdas have already been moved out to the containing class, but we still need to check
            // the containing function (again, see above), so navigate there instead.
            scope is IrDeclaration -> findContainer(scope.parent, approximateToPackage)
            // The only non-declaration parent should be the package.
            else -> scope as? IrPackageFragment
        }
    }
}

/**
 * Calls [onLambda] for each place in the IR subtree where a lambda is passed to an inline function.
 *
 * @param context The backend context
 * @param onLambda The closure to execute for each such lambda. Accepts the lambda expression, the inline function being called,
 *   the parameter of that inline function to which the lambda is passed, and the scope in which the inline function is called.
 */
inline fun IrFile.findInlineLambdas(
    context: JvmBackendContext, crossinline onLambda: (IrFunctionReference, IrFunction, IrValueParameter, IrDeclaration) -> Unit,
) = accept(
    object : IrInlineReferenceLocator(context) {
        override fun visitInlineLambda(
            argument: IrFunctionReference,
            callee: IrFunction,
            parameter: IrValueParameter,
            scope: IrDeclaration,
        ) = onLambda(argument, callee, parameter, scope)
    },
    null,
)

/**
 * Runs [IrInlineScopeResolver] on this [IrFile] and returns the scope resolver instance.
 */
fun IrFile.findInlineCallSites(context: JvmBackendContext) =
    IrInlineScopeResolver(context).apply { accept(this, null) }
