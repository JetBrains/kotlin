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
 * ## Example 1
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
 *
 * ## Example 2
 * Another use-case is "properties" of Java annotations:
 * ```
 * public @interface JavaAnnotation {
 *     public String javaProperty() default ""; // Java method which is considered property in Kotlin,
 *                                              // because methods in Java annotations can't have parameters
 * }
 *
 * fun main(annotation: JavaAnnotation) {
 *    annotation.javaProperty // FirJavaOverriddenSyntheticPropertySymbol
 * }
 * ```
 * The mental model is that in Kotlin world annotations can have only constructor-properties.
 * And Java "overrides" Kotlin's "base Annotation" class (yes, technically there is no base class for annotations).
 */
class FirJavaOverriddenSyntheticPropertySymbol(
    propertyId: CallableId,
    getterId: CallableId
) : FirSyntheticPropertySymbol(propertyId, getterId) {
    override fun copy(): FirSyntheticPropertySymbol = FirJavaOverriddenSyntheticPropertySymbol(callableId, getterId)
}