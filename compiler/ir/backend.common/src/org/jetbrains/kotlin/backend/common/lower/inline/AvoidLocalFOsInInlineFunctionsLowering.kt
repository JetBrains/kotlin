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
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrTypeSubstitutor
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.extractTypeParameters
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

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
                        // The call now targets a member of `superClass`, so the dispatch receiver (currently typed as the local class)
                        // has to be cast to the corresponding supertype. The supertype is computed from the actual receiver type
                        // rather than from the local class declaration, because for a generic local class the declared supertype
                        // is expressed via the class's own type parameters (e.g. `Base<T of Local>`), and those must not leak
                        // into the call site. Substituting through the receiver type gives the specialized one (e.g. `Base<Int>`).
                        // If the receiver type is not a class type, the receiver is left without a cast.
                        val dispatchReceiver = expression.arguments[dispatchParam]
                        val newDispatcherType = (dispatchReceiver?.type as? IrSimpleType)?.substitutedSupertype(superClass.symbol)
                        expression.arguments[dispatchParam] = dispatchReceiver?.implicitCastTo(newDispatcherType)
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

            /**
             * Returns this type viewed as its supertype of class [superClass], with type parameters of all classes in between
             * substituted, or `null` if this type is not a subtype of [superClass].
             *
             * The type of a local class (both a receiver type and a declared supertype, e.g. `L1<T, R>` in `class L2<T> : L1<T>`)
             * may carry more arguments than the class has type constructor parameters: in addition to its own type parameters,
             * the frontend also captures the type parameters of the enclosing declarations, including those outside an enclosing
             * anonymous object (where `extractTypeParameters()` stops). Such extra arguments always come last and are just
             * the captured type parameters themselves, which are in scope at the call site, so they don't need substitution.
             * That's why the generic [org.jetbrains.kotlin.ir.types.AbstractIrTypeSubstitutor.forSuperClass], which requires
             * an exact match of type parameters and arguments, can't be used here.
             */
            private fun IrSimpleType.substitutedSupertype(superClass: IrClassSymbol): IrSimpleType? {
                val clazz = classOrNull ?: return null
                if (clazz == superClass) return this
                val typeParameters = extractTypeParameters(clazz.owner).map { it.symbol }
                val substitutor = IrTypeSubstitutor((typeParameters zip arguments).toMap(), allowEmptySubstitution = true)
                return clazz.owner.superTypes.firstNotNullOfOrNull { superType ->
                    (substitutor.substitute(superType) as? IrSimpleType)?.substitutedSupertype(superClass)
                }
            }

            private fun findOverriddenNonLocalFo(member: IrOverridableDeclaration<*>): IrSymbol? {
                var properMember: IrOverridableDeclaration<*> = member
                while (properMember.isFakeOverride && properMember.isLocal) {
                    properMember = properMember.overriddenSymbols.getOrNull(0)?.owner as? IrOverridableDeclaration<*> ?: break
                }
                return if (properMember === member) null else properMember.symbol
            }
        })
    }
}
