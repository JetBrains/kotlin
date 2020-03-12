/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import org.junit.Test

class ControlFlowTransformTests : AbstractIrTransformTest() {
    private fun controlFlow(
        source: String,
        expectedTransformed: String,
        dumpTree: Boolean = false
    ) = verifyComposeIrTransform(
        ComposeTransforms.DEFAULT xor
        ComposeTransforms.FRAMED_CLASSES xor
        ComposeTransforms.CALLS_AND_EMITS xor
        ComposeTransforms.RESTART_GROUPS or
        ComposeTransforms.CONTROL_FLOW_GROUPS,
        """
            import androidx.compose.Composable

            $source
        """.trimIndent(),
        expectedTransformed,
        dumpTree
    )

    @Test
    fun testIfNonComposable(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    A()
                }
            }
            fun A() { }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              if (x > 0) {
                A()
              }
            }
            fun A() { }
        """
    )

    @Test
    fun testIfWithCallsInBranch(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    A()
                }
            }
            @Composable fun A() { }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              if (x > 0) {
                %composer.startReplaceableGroup(2002223180)
                A(%composer)
                %composer.endReplaceableGroup()
              } else {
                %composer.startReplaceableGroup(2002223093)
                %composer.endReplaceableGroup()
              }
            }
            @Composable
            fun A(%composer: Composer<N>?) { }
        """
    )

    @Test
    fun testIfElseWithCallsInBranch(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    A()
                } else {
                    A()
                }
            }
            @Composable fun A() { }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              if (x > 0) {
                %composer.startReplaceableGroup(2002223180)
                A(%composer)
                %composer.endReplaceableGroup()
              } else {
                %composer.startReplaceableGroup(2002223205)
                A(%composer)
                %composer.endReplaceableGroup()
              }
            }
            @Composable
            fun A(%composer: Composer<N>?) { }
        """
    )

    @Test
    fun testIfWithCallInCondition(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                if (A()) {
                    X()
                } else {
                    X()
                }
            }
            @Composable fun A(): Boolean { return true }
            fun X() { }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              if (A(%composer)) {
                X()
              } else {
                X()
              }
            }
            @Composable
            fun A(%composer: Composer<N>?): Boolean {
              return true
            }
            fun X() { }
        """
    )

    @Test
    fun testIfElseWithCallsInConditions(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                if (A()) {
                    X()
                } else if (A()) {
                    X()
                } else {
                    X()
                }
            }
            @Composable fun A(): Boolean { return true }
            fun X() { }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              %composer.startContainerGroup()
              if (%composer.startReplaceableGroup(2002223173)
              val tmp0_group = A(%composer)
              %composer.endReplaceableGroup()
              tmp0_group) {
                X()
              } else if (%composer.startReplaceableGroup(2002223207)
              val tmp1_group = A(%composer)
              %composer.endReplaceableGroup()
              tmp1_group) {
                X()
              } else if (%composer.startReplaceableGroup(2002223237)
              val tmp2_group = true
              %composer.endReplaceableGroup()
              tmp2_group) {
                X()
              }
              %composer.endContainerGroup()
            }
            @Composable
            fun A(%composer: Composer<N>?): Boolean {
              return true
            }
            fun X() { }
        """
    )

    @Test
    fun testWhenWithSubjectAndNoCalls(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                when (x) {
                    20 -> 8
                    30 -> 10
                    else -> x
                }
            }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              val tmp0_subject = x
              when {
                tmp0_subject == 20 -> {
                  8
                }
                tmp0_subject == 30 -> {
                  10
                }
                else -> {
                  x
                }
              }
            }
        """
    )

    @Test
    fun testWhenWithSubjectAndCalls(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                when (x) {
                    20 -> A()
                    30 -> A()
                    else -> A()
                }
            }
            @Composable fun A() {}
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              val tmp0_subject = x
              when {
                tmp0_subject == 20 -> {
                  %composer.startReplaceableGroup(2002223194)
                  A(%composer)
                  %composer.endReplaceableGroup()
                }
                tmp0_subject == 30 -> {
                  %composer.startReplaceableGroup(2002223212)
                  A(%composer)
                  %composer.endReplaceableGroup()
                }
                else -> {
                  %composer.startReplaceableGroup(2002223232)
                  A(%composer)
                  %composer.endReplaceableGroup()
                }
              }
            }
            @Composable
            fun A(%composer: Composer<N>?) { }
        """
    )

    @Test
    fun testWhenWithSubjectAndCallsWithResult(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                var y = when (x) {
                    20 -> A()
                    30 -> A()
                    else -> A()
                }
            }
            @Composable fun A(): Int { return 10 }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              var y = val tmp0_subject = x
              when {
                tmp0_subject == 20 -> {
                  %composer.startReplaceableGroup(2002223202)
                  val tmp0_group = A(%composer)
                  %composer.endReplaceableGroup()
                  tmp0_group
                }
                tmp0_subject == 30 -> {
                  %composer.startReplaceableGroup(2002223220)
                  val tmp1_group = A(%composer)
                  %composer.endReplaceableGroup()
                  tmp1_group
                }
                else -> {
                  %composer.startReplaceableGroup(2002223240)
                  val tmp2_group = A(%composer)
                  %composer.endReplaceableGroup()
                  tmp2_group
                }
              }
            }
            @Composable
            fun A(%composer: Composer<N>?): Int {
              return 10
            }
        """
    )

    @Test
    fun testWhenWithCalls(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                when {
                    x == 20 -> A()
                    x > 30 -> A()
                    else -> A()
                }
            }
            @Composable fun A() { }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              when {
                x == 20 -> {
                  %composer.startReplaceableGroup(2002223195)
                  A(%composer)
                  %composer.endReplaceableGroup()
                }
                x > 30 -> {
                  %composer.startReplaceableGroup(2002223217)
                  A(%composer)
                  %composer.endReplaceableGroup()
                }
                else -> {
                  %composer.startReplaceableGroup(2002223237)
                  A(%composer)
                  %composer.endReplaceableGroup()
                }
              }
            }
            @Composable
            fun A(%composer: Composer<N>?) { }
        """
    )

    @Test
    fun testWhenWithCallsInConditions(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                when {
                    x == A() -> X()
                    x > A() -> X()
                    else -> X()
                }
            }
            @Composable fun A(): Int { return 10 }
            fun X() {}
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              %composer.startContainerGroup()
              when {
                %composer.startReplaceableGroup(2002223184)
                val tmp0_group = x == A(%composer)
                %composer.endReplaceableGroup()
                tmp0_group -> {
                  X()
                }
                %composer.startReplaceableGroup(2002223208)
                val tmp1_group = x > A(%composer)
                %composer.endReplaceableGroup()
                tmp1_group -> {
                  X()
                }
                %composer.startReplaceableGroup(2002223239)
                val tmp2_group = true
                %composer.endReplaceableGroup()
                tmp2_group -> {
                  X()
                }
              }
              %composer.endContainerGroup()
            }
            @Composable
            fun A(%composer: Composer<N>?): Int {
              return 10
            }
            fun X() { }
        """
    )

    @Test
    fun testSafeCall(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int?) {
                x?.A()
            }
            @Composable fun Int.A() { }
        """,
        """
            @Composable
            fun Example(x: Int?, %composer: Composer<N>?) {
              val tmp0_safe_receiver = x
              when {
                tmp0_safe_receiver == null -> {
                  %composer.startReplaceableGroup(2002223173)
                  val tmp0_group = null
                  %composer.endReplaceableGroup()
                  tmp0_group
                }
                else -> {
                  %composer.startReplaceableGroup(2002223173)
                  tmp0_safe_receiver.A(%composer)
                  %composer.endReplaceableGroup()
                }
              }
            }
            @Composable
            fun Int.A(%composer: Composer<N>?) { }
        """
    )

    @Test
    fun testElvis(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int?) {
                val y = x ?: A()
            }
            @Composable fun A(): Int { return 10 }
        """,
        """
            @Composable
            fun Example(x: Int?, %composer: Composer<N>?) {
              val y = val tmp0_elvis_lhs = x
              when {
                tmp0_elvis_lhs == null -> {
                  %composer.startReplaceableGroup(2002223183)
                  val tmp0_group = A(%composer)
                  %composer.endReplaceableGroup()
                  tmp0_group
                }
                else -> {
                  %composer.startReplaceableGroup(2002223178)
                  val tmp1_group = tmp0_elvis_lhs
                  %composer.endReplaceableGroup()
                  tmp1_group
                }
              }
            }
            @Composable
            fun A(%composer: Composer<N>?): Int {
              return 10
            }
        """
    )

    @Test
    fun testForLoopWithCallsInBody(): Unit = controlFlow(
        """
            @Composable
            fun Example(items: List<Int>) {
                for (i in items) {
                    A(i)
                }
            }
            @Composable fun A(x: Int) { }
        """,
        """
            @Composable
            fun Example(items: List<Int>, %composer: Composer<N>?) {
              val tmp0_iterator = items.iterator()
              %composer.startContainerGroup()
              while (tmp0_iterator.hasNext()) {
                %composer.startReplaceableGroup(2002223179)
                val i = tmp0_iterator.next()
                A(i, %composer)
                %composer.endReplaceableGroup()
              }
              %composer.endContainerGroup()
            }
            @Composable
            fun A(x: Int, %composer: Composer<N>?) { }
        """
    )

    @Test
    fun testForLoopWithCallsInSubject(): Unit = controlFlow(
        """
            @Composable
            fun Example(items: List<Int>) {
                for (i in A()) {
                    print(i)
                }
            }
            @Composable fun A(): List<Int> { return listOf(1, 2, 3) }
        """,
        """
            @Composable
            fun Example(items: List<Int>, %composer: Composer<N>?) {
              val tmp0_iterator = A(%composer).iterator()
              while (tmp0_iterator.hasNext()) {
                val i = tmp0_iterator.next()
                print(i)
              }
            }
            @Composable
            fun A(%composer: Composer<N>?): List<Int> {
              return listOf(1, 2, 3)
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInBody(): Unit = controlFlow(
        """
            @Composable
            fun Example(items: MutableList<Int>) {
                while (items.isNotEmpty()) {
                    val item = items.removeAt(items.size - 1)
                    A(item)
                }
            }
            @Composable fun A(x: Int) { }
        """,
        """
            @Composable
            fun Example(items: MutableList<Int>, %composer: Composer<N>?) {
              %composer.startContainerGroup()
              while (items.isNotEmpty()) {
                %composer.startReplaceableGroup(2002223213)
                val item = items.removeAt(items.<get-size>() - 1)
                A(item, %composer)
                %composer.endReplaceableGroup()
              }
              %composer.endContainerGroup()
            }
            @Composable
            fun A(x: Int, %composer: Composer<N>?) { }
        """
    )

    @Test
    fun testWhileLoopWithCallsInCondition(): Unit = controlFlow(
        """
            @Composable
            fun Example() {
                while (A()) {
                    print("hello world")
                }
            }
            @Composable fun A(): Boolean { return true }
        """,
        """
            @Composable
            fun Example(%composer: Composer<N>?) {
              %composer.startContainerGroup()
              while (%composer.startReplaceableGroup(2002223170)
              val tmp0_group = A(%composer)
              %composer.endReplaceableGroup()
              tmp0_group) {
                print("hello world")
              }
              %composer.endContainerGroup()
            }
            @Composable
            fun A(%composer: Composer<N>?): Boolean {
              return true
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInConditionAndBody(): Unit = controlFlow(
        """
            @Composable
            fun Example() {
                while (A()) {
                    B()
                }
            }
            @Composable fun A(): Boolean { return true }
            @Composable fun B() { }
        """,
        """
            @Composable
            fun Example(%composer: Composer<N>?) {
              %composer.startContainerGroup()
              while (%composer.startReplaceableGroup(2002223170)
              val tmp0_group = A(%composer)
              %composer.endReplaceableGroup()
              tmp0_group) {
                %composer.startReplaceableGroup(2002223175)
                B(%composer)
                %composer.endReplaceableGroup()
              }
              %composer.endContainerGroup()
            }
            @Composable
            fun A(%composer: Composer<N>?): Boolean {
              return true
            }
            @Composable
            fun B(%composer: Composer<N>?) { }
        """
    )
}