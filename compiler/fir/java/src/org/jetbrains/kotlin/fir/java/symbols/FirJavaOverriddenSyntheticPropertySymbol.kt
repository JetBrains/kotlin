/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.symbols

import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.name.CallableId

/**
 * This is a synthetic property symbol created for Java getter overriding Kotlin property.
 *
 * Frontend IR creates such kind a symbol when a Java class is asked for a property which
 * exists in one of its base Kotlin classes, and the Java class itself contains the bound getter.
 *
 * The typical example:
 *
 * ```
 * abstract class SomeKotlinClass {
 *     abstract val foo: Int
 * }
 *
 * public class SomeJavaClass extends SomeKotlinClass {
 *     @Override
 *     public int getFoo() { return 42; }
 * }
 * ```
 */
class FirJavaOverriddenSyntheticPropertySymbol(
    propertyId: CallableId,
    getterId: CallableId
) : FirSyntheticPropertySymbol(propertyId, getterId) {
    override fun copy(): FirSyntheticPropertySymbol = FirJavaOverriddenSyntheticPropertySymbol(callableId, getterId)
}