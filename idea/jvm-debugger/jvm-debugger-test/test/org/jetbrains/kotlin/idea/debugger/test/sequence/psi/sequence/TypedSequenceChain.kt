/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.debugger.test.sequence.psi.sequence

import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import org.jetbrains.kotlin.idea.debugger.sequence.lib.sequence.KotlinSequenceSupportProvider
import org.jetbrains.kotlin.idea.debugger.test.sequence.psi.TypedChainTestCase
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinSequenceTypes
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class TypedSequenceChain : TypedChainTestCase("sequence/positive/types") {
    override val kotlinChainBuilder: StreamChainBuilder = KotlinSequenceSupportProvider().chainBuilder

    fun testAny() = doTest(KotlinSequenceTypes.ANY)
    fun testNullableAny() = doTest(KotlinSequenceTypes.NULLABLE_ANY)

    fun testBoolean() = doTest(KotlinSequenceTypes.BOOLEAN)
    fun testNullableBoolean() = doTest(KotlinSequenceTypes.NULLABLE_ANY)

    fun testByte() = doTest(KotlinSequenceTypes.BYTE)
    fun testNullableByte() = doTest(KotlinSequenceTypes.NULLABLE_ANY)

    fun testShort() = doTest(KotlinSequenceTypes.SHORT)
    fun testNullableShort() = doTest(KotlinSequenceTypes.NULLABLE_ANY)

    fun testInt() = doTest(KotlinSequenceTypes.INT)
    fun testNullableInt() = doTest(KotlinSequenceTypes.NULLABLE_ANY)

    fun testLong() = doTest(KotlinSequenceTypes.LONG)
    fun testNullableLong() = doTest(KotlinSequenceTypes.NULLABLE_ANY)

    fun testFloat() = doTest(KotlinSequenceTypes.FLOAT)
    fun testNullableFloat() = doTest(KotlinSequenceTypes.NULLABLE_ANY)

    fun testDouble() = doTest(KotlinSequenceTypes.DOUBLE)
    fun testNullableDouble() = doTest(KotlinSequenceTypes.NULLABLE_ANY)

    fun testChar() = doTest(KotlinSequenceTypes.CHAR)
    fun testNullableChar() = doTest(KotlinSequenceTypes.NULLABLE_ANY)

    fun testNullableAnyToPrimitive() = doTest(KotlinSequenceTypes.NULLABLE_ANY, KotlinSequenceTypes.BOOLEAN)
    fun testPrimitiveToNullableAny() = doTest(KotlinSequenceTypes.INT, KotlinSequenceTypes.NULLABLE_ANY)

    fun testAnyToPrimitive() = doTest(KotlinSequenceTypes.ANY, KotlinSequenceTypes.INT)
    fun testPrimitiveToAny() = doTest(KotlinSequenceTypes.INT, KotlinSequenceTypes.ANY)

    fun testNullableToNotNull() = doTest(KotlinSequenceTypes.NULLABLE_ANY, KotlinSequenceTypes.INT)
    fun testNotNullToNullable() = doTest(KotlinSequenceTypes.DOUBLE, KotlinSequenceTypes.NULLABLE_ANY)

    fun testFewTransitions1() =
        doTest(KotlinSequenceTypes.BYTE, KotlinSequenceTypes.ANY, KotlinSequenceTypes.NULLABLE_ANY, KotlinSequenceTypes.INT)

    fun testFewTransitions2() =
        doTest(KotlinSequenceTypes.CHAR, KotlinSequenceTypes.BOOLEAN, KotlinSequenceTypes.DOUBLE, KotlinSequenceTypes.ANY)
}