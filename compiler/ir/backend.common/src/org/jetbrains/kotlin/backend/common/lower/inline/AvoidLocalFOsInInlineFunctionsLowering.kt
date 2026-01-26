/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrRichCallableReference
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrRichPropertyReference
import org.jetbrains.kotlin.ir.expressions.implicitCastTo
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.getAllSubstitutedSupertypes
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid

/**
 * KLIB based backends have a problem linking a reference to fake override member of local class, if the class happened to be inlined (KT-72296).
 * It could theoretically be fixed by creating a member signature that would be more resistant to inlining.
 * But instead, we just avoid references to local FOs altogether, by replacing them with the real overridden declaration of a super type.
 *
 * Example:
 * ```
 * open class A { fun foo() {} }
 * inline fun foo() {
 *   val x = object : A() {}
 *   x.foo()
 * }
 * ```
 * will be replaced roughly by:
 *```
 * open class A { fun foo() {} }
 * inline fun foo() {
 *   val x = object : A() {}
 *   (x as A).foo() /* A::foo */
 * }
 * ```
 */
class AvoidLocalFOsInInlineFunctionsLowering(val context: LoweringContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val function = ((container as? IrValueParameter)?.parent ?: container) as? IrFunction ?: return
        if (!function.isInline) {
            return
        }

        irBody.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                val callee = expression.symbol.owner
                findOverriddenNonLocalFo(callee)?.let { newSymbol ->
                    expression.symbol = newSymbol as IrSimpleFunctionSymbol
                    newSymbol.owner.dispatchReceiverParameter?.let { dispatchParam ->
                        val superClass = newSymbol.owner.parentAsClass
                        val newDispatcherType = getAllSubstitutedSupertypes(callee.parentAsClass).first { it.classifier == superClass.symbol }
                        expression.arguments[dispatchParam] = expression.arguments[dispatchParam]?.implicitCastTo(newDispatcherType)
                    }
                }

                super.visitMemberAccess(expression)
            }

            // Note: visiting the old implementation of references (IrCallableReference) is not required
            // as those won't be found in functions inlined of the 1st stage, which is where the original problem appears.
            override fun visitRichCallableReference(expression: IrRichCallableReference<*>) {
                super.visitRichCallableReference(expression)

                val target = expression.reflectionTargetSymbol?.owner as? IrOverridableDeclaration<*> ?: return
                findOverriddenNonLocalFo(target)?.let {
                    when (expression) {
                        is IrRichFunctionReference -> expression.reflectionTargetSymbol = it as IrSimpleFunctionSymbol
                        is IrRichPropertyReference -> expression.reflectionTargetSymbol = it as IrPropertySymbol
                    }
                }
            }

            private fun findOverriddenNonLocalFo(member: IrOverridableDeclaration<*>): IrSymbol? {
                var properMember: IrOverridableDeclaration<*> = member
                while (properMember.isLocal && properMember.isFakeOverride) {
                    properMember = properMember.overriddenSymbols.getOrNull(0)?.owner as? IrOverridableDeclaration<*> ?: break
                }
                return if (properMember === member) null else properMember.symbol
            }
        })
    }
}