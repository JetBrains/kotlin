/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.backend.common.ir.inlineDeclaration
import org.jetbrains.kotlin.backend.common.ir.isFunctionInlining
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName

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

    abstract fun visitInlineLambda(argument: IrFunctionReference, callee: IrFunction, parameter: IrValueParameter, scope: IrDeclaration)
}

class IrInlineScopeResolver(context: JvmBackendContext) : IrInlineReferenceLocator(context) {
    private class CallSite(val parent: IrElement?, val approximateToPackage: Boolean)

    private val inlineCallSites = mutableMapOf<IrFunction, CallSite>()
    private val inlineFunctionCallSites = mutableMapOf<IrFunction, Set<IrElement>>()

    override fun visitInlineLambda(argument: IrFunctionReference, callee: IrFunction, parameter: IrValueParameter, scope: IrDeclaration) {
        // suspendCoroutine and suspendCoroutineUninterceptedOrReturn accept crossinline lambdas to disallow non-local returns,
        // but these lambdas are effectively inline
        inlineCallSites[argument.symbol.owner] = CallSite(scope, parameter.isCrossinline && !callee.isCoroutineIntrinsic())
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: IrDeclaration?) {
        if (declaration.isPrivateInline) {
            inlineFunctionCallSites.putIfAbsent(declaration, mutableSetOf())
        }
        super.visitSimpleFunction(declaration, data)
    }

    override fun visitCall(expression: IrCall, data: IrDeclaration?) {
        val callee = expression.symbol.owner
        if (callee.isPrivateInline && data != null) {
            (inlineFunctionCallSites.getOrPut(callee) { mutableSetOf() } as MutableSet).add(data)
        }
        super.visitCall(expression, data)
    }

    override fun visitBlock(expression: IrBlock, data: IrDeclaration?) {
        if (expression is IrInlinedFunctionBlock && expression.isFunctionInlining()) {
            val callee = expression.inlineDeclaration
            if (callee is IrSimpleFunction && callee.isPrivateInline && data != null) {
                (inlineFunctionCallSites.getOrPut(callee) { mutableSetOf() } as MutableSet).add(data)
            }
        }

        super.visitBlock(expression, data)
    }

    private inline val IrSimpleFunction.isPrivateInline
        get() = isInline && DescriptorVisibilities.isPrivate(visibility)

    private fun IrFunction.isCoroutineIntrinsic(): Boolean =
        (name.asString() == "suspendCoroutine" && getPackageFragment().fqName == FqName("kotlin.coroutines")) ||
                (name.asString() == "suspendCoroutineUninterceptedOrReturn" && getPackageFragment().fqName == FqName("kotlin.coroutines.intrinsics"))

    fun findContainer(scope: IrElement): IrDeclarationContainer? =
        findContainer(scope, approximateToPackage = false)

    // Get the class from which all accesses in the current scope will be done after bytecode generation.
    // If the current scope is a crossinline lambda, this is not possible, as the lambda maybe inlined
    // into some other class; in that case, get at least the package.
    private tailrec fun findContainer(context: IrElement?, approximateToPackage: Boolean): IrDeclarationContainer? {
        val callSite = inlineCallSites[context]
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
            callSite != null -> findContainer(callSite.parent, approximateToPackage || callSite.approximateToPackage)
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
            context is IrFunction && context.isInline -> {
                val callSites = inlineFunctionCallSites[context] ?: return null
                // Mark to avoid infinite recursion on self-recursive inline functions (those are only
                // detected reliably by codegen; frontend only filters out simple cases).
                inlineCallSites[context] = CallSite(null, approximateToPackage = false)
                val commonCallSite = when {
                    callSites.isEmpty() -> CallSite(context.parent, false)
                    callSites.size == 1 -> CallSite(callSites.single(), false)
                    else -> {
                        @Suppress("NON_TAIL_RECURSIVE_CALL")
                        val results = callSites.map { findContainer(it, approximateToPackage = false) ?: return null }
                        // If all call sites are within a single class, use it. Otherwise, all scopes must be within
                        // the current file's package.
                        val single = results.first().takeIf { results.all { other -> it === other } }
                        CallSite(single ?: context.parent, single == null)
                    }
                }
                inlineCallSites[context] = commonCallSite
                findContainer(commonCallSite.parent, approximateToPackage || commonCallSite.approximateToPackage)
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
            context is IrClass && !approximateToPackage -> context
            // Inline lambdas have already been moved out to the containing class, but we still need to check
            // the containing function (again, see above), so navigate there instead.
            context is IrDeclaration -> findContainer(context.parent, approximateToPackage)
            // The only non-declaration parent should be the package.
            else -> context as? IrPackageFragment
        }
    }
}

inline fun IrFile.findInlineLambdas(
    context: JvmBackendContext, crossinline onLambda: (IrFunctionReference, IrFunction, IrValueParameter, IrDeclaration) -> Unit
) = accept(object : IrInlineReferenceLocator(context) {
    override fun visitInlineLambda(argument: IrFunctionReference, callee: IrFunction, parameter: IrValueParameter, scope: IrDeclaration) =
        onLambda(argument, callee, parameter, scope)
}, null)

fun IrFile.findInlineCallSites(context: JvmBackendContext) =
    IrInlineScopeResolver(context).apply { accept(this, null) }
