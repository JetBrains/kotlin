/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

/**
 * This class represents possibilities of interpretation of fake overrides in classes in JVM backend.
 * The main use case is to figure out if a declaration represented by a fake override really exists in the JVM class file, for example
 * to generate a super call to it correctly.
 */
sealed class ClassFakeOverrideReplacement {
    /**
     * The fake override should be replaced by a "DefaultImpls redirection" method. There are two cases when this is happening:
     *
     * 1) In the `-jvm-default=disable` mode, we generate a method which calls the method from the super-interface's DefaultImpls.
     * 2) In the `-jvm-default=enable` mode (without `@JvmDefaultWithoutCompatibility` on the class), we generate a method which calls
     *    the super method.
     *
     * For example:
     *
     *     interface A {
     *         fun f(s: String) {}
     *     }
     *     class B : A
     *
     * Suppose that `A` is compiled with `-jvm-default=disable`.
     *
     * If `B` is compiled with `-jvm-default=disable`, the fake override `B.f` will be replaced by a method which calls `A$DefaultImpls.f`.
     *
     * If `B` is compiled with `-jvm-default=enable`, the fake override will be replaced by a method which calls `super.f`.
     *
     * @param newFunction the newly created non-fake function in the same class, whose body (in case this class is from sources) will be
     *   filled by the corresponding lowering phase (`InheritedDefaultMethodsOnClassesLowering`).
     * @param superFunction the non-abstract function from an immediate super-interface which should be called in the body of [newFunction].
     * @param callee in case of DefaultImpls redirection, the method from the super-interface's DefaultImpls class, corresponding
     *   to [superFunction], otherwise [superFunction].
     */
    data class DefaultImplsRedirection(
        val newFunction: IrSimpleFunction,
        val superFunction: IrSimpleFunction,
        val callee: IrSimpleFunction,
    ) : ClassFakeOverrideReplacement()

    /**
     * "Default compatibility bridge" is a method which is generated in a class in `-jvm-default=enable/no-compatibility` modes, to keep
     * behavior in diamond hierarchies the same as in the `-jvm-default=disable` mode. Example:
     *
     *     interface Base {
     *         fun foo(): String = "Fail"
     *     }
     *     open class Left : Base
     *
     *     interface Right : Base {
     *         override fun foo(): String = "OK"
     *     }
     *     class Bottom : Left(), Right
     *
     * If Base and Left are compiled with `-jvm-default=disable`, but Right and Bottom are compiled with `enable/no-compatibility`,
     * we generate a _default compatibility bridge_ in Bottom which calls `super<Right>.foo()`. Without this bridge, calls to `Bottom.foo()`
     * would result in "Fail" because there's a DefaultImpls bridge in Left which is inherited in Bottom, and class methods win over default
     * interface methods in JVM during call resolution.
     *
     * @param newFunction the newly created non-fake function in the same class, whose body (in case this class is from sources) will be
     *   filled by the corresponding lowering phase (`GenerateJvmDefaultCompatibilityBridges`).
     * @param superFunction the non-abstract function from an immediate super-interface which should be called in the body of [newFunction].
     */
    data class DefaultCompatibilityBridge(
        val newFunction: IrSimpleFunction, val superFunction: IrSimpleFunction,
    ) : ClassFakeOverrideReplacement()

    data object None : ClassFakeOverrideReplacement()
}
