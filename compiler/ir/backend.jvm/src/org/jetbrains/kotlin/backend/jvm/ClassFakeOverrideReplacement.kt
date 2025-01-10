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
     * This class means that a fake override should be replaced by a "DefaultImpls redirection" method. This is done only in the
     * `-Xjvm-default=disable` mode. For example:
     *
     *     interface A {
     *         fun f(s: String) {}
     *     }
     *     class B : A
     *
     * If this is compiled with `-Xjvm-default=disable`, the fake override for `B.f` will be replaced by a DefaultImpls redirection:
     * a method will be generated in `B` that calls `A$DefaultImpls.f`.
     *
     * @param newFunction the newly created non-fake function in the same class, whose body (in case this class is from sources) will be
     *   filled by the corresponding lowering phase (`InheritedDefaultMethodsOnClassesLowering`).
     * @param superFunction the non-abstract function from an immediate super-interface which should be called in the body of [newFunction].
     */
    data class DefaultImplsRedirection(
        val newFunction: IrSimpleFunction, val superFunction: IrSimpleFunction,
    ) : ClassFakeOverrideReplacement()

    /**
     * "Default compatibility bridge" is a method which is generated in a class in `-Xjvm-default=all/all-compatibility` modes, to keep
     * behavior in diamond hierarchies the same as in the `-Xjvm-default=disable` mode. Example:
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
     * If Base and Left are compiled with `-Xjvm-default=disable`, but Right and Bottom are compiled with `all/all-compatibility`,
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
