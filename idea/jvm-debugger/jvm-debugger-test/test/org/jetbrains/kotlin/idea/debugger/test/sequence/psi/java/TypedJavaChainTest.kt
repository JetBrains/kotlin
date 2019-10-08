/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.debugger.test.sequence.psi.java

import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import org.jetbrains.kotlin.idea.debugger.sequence.lib.java.JavaStandardLibrarySupportProvider
import org.jetbrains.kotlin.idea.debugger.test.sequence.psi.TypedChainTestCase
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinSequenceTypes.DOUBLE
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinSequenceTypes.INT
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinSequenceTypes.LONG
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinSequenceTypes.NULLABLE_ANY
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class TypedJavaChainTest : TypedChainTestCase("streams/positive/types") {
    override val kotlinChainBuilder: StreamChainBuilder = JavaStandardLibrarySupportProvider().chainBuilder

    fun testOneCall() = doTest(NULLABLE_ANY)
    fun testMapToSame() = doTest(NULLABLE_ANY, NULLABLE_ANY)
    fun testPrimitiveOneCall() = doTest(INT)
    fun testPrimitiveMapToSame() = doTest(LONG, LONG)

    fun testMapToPrimitive() = doTest(NULLABLE_ANY, DOUBLE)
    fun testMapToObj() = doTest(DOUBLE, NULLABLE_ANY)
    fun testMapPrimitiveToPrimitive() = doTest(LONG, INT)

    fun testFewTransitions() = doTest(NULLABLE_ANY, INT, NULLABLE_ANY, LONG, DOUBLE)
}
