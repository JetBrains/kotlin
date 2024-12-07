/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.util.StubGeneratorExtensions
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType

open class GeneratorExtensions : StubGeneratorExtensions() {
    open val samConversion: SamConversion
        get() = SamConversion

    open class SamConversion {
        open fun isPlatformSamType(type: KotlinType): Boolean = false

        open fun isCarefulApproximationOfContravariantProjection(): Boolean = false

        companion object Instance : SamConversion()
    }

    open fun computeFieldVisibility(descriptor: PropertyDescriptor): DescriptorVisibility? = null

    open fun getParentClassStaticScope(descriptor: ClassDescriptor): MemberScope? = null

    open fun createCustomSuperConstructorCall(
        ktPureClassOrObject: KtPureClassOrObject,
        descriptor: ClassDescriptor,
        context: GeneratorContext,
    ): IrDelegatingConstructorCall? = null

    open val shouldPreventDeprecatedIntegerValueTypeLiteralConversion: Boolean
        get() = false

    open fun getPreviousScripts(): List<IrScriptSymbol>? = null
    open val lowerScriptToClass: Boolean get() = true

    open fun unwrapSyntheticJavaProperty(descriptor: PropertyDescriptor): Pair<FunctionDescriptor, FunctionDescriptor?>? = null

    open fun remapDebuggerFieldPropertyDescriptor(propertyDescriptor: PropertyDescriptor): PropertyDescriptor = propertyDescriptor

    open val parametersAreAssignable: Boolean
        get() = false

    /**
     * Enables improved source offsets for the desugared IR generated for
     * destructuring declarations.
     *
     * Local variables defined by destructuring, e.g.
     *
     *   val (x, y) = destructee()
     *
     * is represented in IR by
     *
     *  block {
     *   val containerTmp = destructee()
     *   val x = containerTmp.component1()
     *   val y = containerTmp.component2()
     *  }
     *
     * When [debugInfoOnlyOnVariablesInDestructuringDeclarations] is `false`, the
     * access to `containerTmp` in the calls to `component` calls are given source
     * positions corresponding to `destructee()` which causes multi-line
     * destructuring declarations to step back and forth between the variables being
     * declared and the right-hand side, implying the repeated evaluation of the
     * right-hand side.
     *
     * When `true`, only the stores to `x` and `y` in the generated code are are
     * given source offsets, the source offsets of `x` and `y` in the original
     * declaration, giving fewer, more accurate steps, that are closer to the JVM
     * backend in behavior.
     */
    open val debugInfoOnlyOnVariablesInDestructuringDeclarations: Boolean
        get() = false
}
