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
        """
            import androidx.compose.Composable

            @Composable fun A() {}
            @Composable fun B(): Boolean { return true }
            @Composable fun R(): Int { return 10 }
            @Composable fun P(x: Int) { }
            @Composable fun Int.A() { }
            @Composable fun L(): List<Int> { return listOf(1, 2, 3) }
            fun NA() { }
            fun NB(): Boolean { return true }
            fun NR(): Int { return 10 }
        """.trimIndent(),
        dumpTree
    )

    @Test
    fun testIfNonComposable(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    NA()
                }
            }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              if (x > 0) {
                NA()
              }
              %composer.endReplaceableGroup()
            }
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
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              if (x > 0) {
                %composer.startReplaceableGroup(2002223180)
                A(%composer)
                %composer.endReplaceableGroup()
              } else {
                %composer.startReplaceableGroup(2002223093)
                %composer.endReplaceableGroup()
              }
              %composer.endReplaceableGroup()
            }
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
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              if (x > 0) {
                %composer.startReplaceableGroup(2002223180)
                A(%composer)
                %composer.endReplaceableGroup()
              } else {
                %composer.startReplaceableGroup(2002223205)
                A(%composer)
                %composer.endReplaceableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testIfWithCallInCondition(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                if (B()) {
                    NA()
                } else {
                    NA()
                }
            }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              if (B(%composer)) {
                NA()
              } else {
                NA()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testIfElseWithCallsInConditions(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                if (B()) {
                    NA()
                } else if (B()) {
                    NA()
                } else {
                    NA()
                }
            }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              if (%composer.startReplaceableGroup(2002223173)
              val tmp0_group = B(%composer)
              %composer.endReplaceableGroup()
              tmp0_group) {
                NA()
              } else if (%composer.startReplaceableGroup(2002223208)
              val tmp1_group = B(%composer)
              %composer.endReplaceableGroup()
              tmp1_group) {
                NA()
              } else if (%composer.startReplaceableGroup(2002223239)
              %composer.endReplaceableGroup()
              true) {
                NA()
              }
              %composer.endReplaceableGroup()
            }
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
              %composer.startReplaceableGroup(2002223143)
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
              %composer.endReplaceableGroup()
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
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
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
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhenWithSubjectAndCallsWithResult(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                var y = when (x) {
                    20 -> R()
                    30 -> R()
                    else -> R()
                }
            }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              var y = val tmp0_subject = x
              when {
                tmp0_subject == 20 -> {
                  %composer.startReplaceableGroup(2002223202)
                  val tmp0_group = R(%composer)
                  %composer.endReplaceableGroup()
                  tmp0_group
                }
                tmp0_subject == 30 -> {
                  %composer.startReplaceableGroup(2002223220)
                  val tmp1_group = R(%composer)
                  %composer.endReplaceableGroup()
                  tmp1_group
                }
                else -> {
                  %composer.startReplaceableGroup(2002223240)
                  val tmp2_group = R(%composer)
                  %composer.endReplaceableGroup()
                  tmp2_group
                }
              }
              %composer.endReplaceableGroup()
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
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
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
              %composer.endReplaceableGroup()
            }

        """
    )

    @Test
    fun testWhenWithCallsInConditions(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                when {
                    x == R() -> NA()
                    x > R() -> NA()
                    else -> NA()
                }
            }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              when {
                %composer.startReplaceableGroup(2002223184)
                val tmp0_group = x == R(%composer)
                %composer.endReplaceableGroup()
                tmp0_group -> {
                  NA()
                }
                %composer.startReplaceableGroup(2002223209)
                val tmp1_group = x > R(%composer)
                %composer.endReplaceableGroup()
                tmp1_group -> {
                  NA()
                }
                %composer.startReplaceableGroup(2002223241)
                %composer.endReplaceableGroup()
                true -> {
                  NA()
                }
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testSafeCall(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int?) {
                x?.A()
            }
        """,
        """
            @Composable
            fun Example(x: Int?, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              val tmp0_safe_receiver = x
              when {
                tmp0_safe_receiver == null -> {
                  %composer.startReplaceableGroup(2002223173)
                  %composer.endReplaceableGroup()
                  null
                }
                else -> {
                  %composer.startReplaceableGroup(2002223173)
                  tmp0_safe_receiver.A(%composer)
                  %composer.endReplaceableGroup()
                }
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testElvis(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int?) {
                val y = x ?: R()
            }
        """,
        """
            @Composable
            fun Example(x: Int?, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              val y = val tmp0_elvis_lhs = x
              when {
                tmp0_elvis_lhs == null -> {
                  %composer.startReplaceableGroup(2002223183)
                  val tmp0_group = R(%composer)
                  %composer.endReplaceableGroup()
                  tmp0_group
                }
                else -> {
                  %composer.startReplaceableGroup(2002223178)
                  %composer.endReplaceableGroup()
                  tmp0_elvis_lhs
                }
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testForLoopWithCallsInBody(): Unit = controlFlow(
        """
            @Composable
            fun Example(items: List<Int>) {
                for (i in items) {
                    P(i)
                }
            }
        """,
        """
            @Composable
            fun Example(items: List<Int>, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              val tmp0_iterator = items.iterator()
              while (tmp0_iterator.hasNext()) {
                %composer.startReplaceableGroup(2002223179)
                val i = tmp0_iterator.next()
                P(i, %composer)
                %composer.endReplaceableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testForLoopWithCallsInSubject(): Unit = controlFlow(
        """
            @Composable
            fun Example() {
                for (i in L()) {
                    print(i)
                }
            }
        """,
        """
            @Composable
            fun Example(%composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              val tmp0_iterator = L(%composer).iterator()
              while (tmp0_iterator.hasNext()) {
                val i = tmp0_iterator.next()
                print(i)
              }
              %composer.endReplaceableGroup()
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
                    P(item)
                }
            }
        """,
        """
            @Composable
            fun Example(items: MutableList<Int>, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              while (items.isNotEmpty()) {
                %composer.startReplaceableGroup(2002223213)
                val item = items.removeAt(items.<get-size>() - 1)
                P(item, %composer)
                %composer.endReplaceableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInCondition(): Unit = controlFlow(
        """
            @Composable
            fun Example() {
                while (B()) {
                    print("hello world")
                }
            }
        """,
        """
            @Composable
            fun Example(%composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              while (%composer.startReplaceableGroup(2002223170)
              val tmp0_group = B(%composer)
              %composer.endReplaceableGroup()
              tmp0_group) {
                print("hello world")
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInConditionAndBody(): Unit = controlFlow(
        """
            @Composable
            fun Example() {
                while (B()) {
                    A()
                }
            }
        """,
        """
            @Composable
            fun Example(%composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              while (%composer.startReplaceableGroup(2002223170)
              val tmp0_group = B(%composer)
              %composer.endReplaceableGroup()
              tmp0_group) {
                %composer.startReplaceableGroup(2002223175)
                A(%composer)
                %composer.endReplaceableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testEarlyReturnWithCallsBeforeButNotAfter(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    A()
                    return
                }
                print("hello")
            }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              if (x > 0) {
                %composer.startReplaceableGroup(2002223180)
                A(%composer)
                %composer.endReplaceableGroup()
                %composer.endReplaceableGroup()
                return
              } else {
                %composer.startReplaceableGroup(2002223093)
                %composer.endReplaceableGroup()
              }
              print("hello")
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testEarlyReturnWithCallsAfterButNotBefore(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    return
                }
                A()
            }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              if (x > 0) {
                %composer.endReplaceableGroup()
                return
              }
              A(%composer)
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testEarlyReturnValue(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int): Int {
                if (x > 0) {
                    A()
                    return 1
                }
                return 2
            }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?): Int {
              %composer.startReplaceableGroup(2002223143)
              if (x > 0) {
                %composer.startReplaceableGroup(2002223185)
                A(%composer)
                %composer.endReplaceableGroup()
                %composer.endReplaceableGroup()
                return 1
              } else {
                %composer.startReplaceableGroup(2002223093)
                %composer.endReplaceableGroup()
              }
              %composer.endReplaceableGroup()
              return 2
            }
        """
    )

    @Test
    fun testEarlyReturnValueWithCallsAfter(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int): Int {
                if (x > 0) {
                    return 1
                }
                A()
                return 2
            }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?): Int {
              %composer.startReplaceableGroup(2002223143)
              if (x > 0) {
                %composer.endReplaceableGroup()
                return 1
              }
              A(%composer)
              %composer.endReplaceableGroup()
              return 2
            }
        """
    )

    @Test
    fun testReturnCallValue(): Unit = controlFlow(
        """
            @Composable
            fun Example(): Int {
                A()
                return R()
            }
        """,
        """
            @Composable
            fun Example(%composer: Composer<N>?): Int {
              %composer.startReplaceableGroup(2002223143)
              A(%composer)
              val tmp0_return = R(%composer)
              %composer.endReplaceableGroup()
              return tmp0_return
            }
        """
    )

    @Test
    fun testEarlyReturnCallValue(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int): Int {
                if (x > 0) {
                    return R()
                }
                return R()
            }
        """,
        """
            @Composable
            fun Example(x: Int, %composer: Composer<N>?): Int {
              %composer.startReplaceableGroup(2002223143)
              if (x > 0) {
                %composer.startReplaceableGroup(2002223185)
                val tmp0_return = R(%composer)
                %composer.endReplaceableGroup()
                %composer.endReplaceableGroup()
                return tmp0_return
              } else {
                %composer.startReplaceableGroup(2002223093)
                %composer.endReplaceableGroup()
              }
              val tmp1_return = R(%composer)
              %composer.endReplaceableGroup()
              return tmp1_return
            }
        """
    )

    @Test
    fun testReturnFromLoop(): Unit = controlFlow(
        """
            @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    P(i)
                    if (i == 0) {
                        P(i)
                        return
                    } else {
                        P(i)
                    }
                    P(i)
                }
            }
        """,
        """
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              while (items.hasNext()) {
                %composer.startReplaceableGroup(2002223207)
                val i = items.next()
                P(i, %composer)
                if (i == 0) {
                  %composer.startReplaceableGroup(2002223271)
                  P(i, %composer)
                  %composer.endReplaceableGroup()
                  %composer.endReplaceableGroup()
                  %composer.endReplaceableGroup()
                  return
                } else {
                  %composer.startReplaceableGroup(2002223324)
                  P(i, %composer)
                  %composer.endReplaceableGroup()
                }
                P(i, %composer)
                %composer.endReplaceableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testBreakWithCallsAfter(): Unit = controlFlow(
            """
            @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    if (i == 0) {
                        break
                    }
                    P(i)
                }
            }
        """,
        """
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              while (items.hasNext()) {
                %composer.startReplaceableGroup(2002223207)
                val i = items.next()
                if (i == 0) {
                  %composer.endReplaceableGroup()
                  break
                }
                P(i, %composer)
                %composer.endReplaceableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
        )

    @Test
    fun testBreakWithCallsBefore(): Unit = controlFlow(
        """
            @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    P(i)
                    if (i == 0) {
                        break
                    }
                }
            }
        """,
        """
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              while (items.hasNext()) {
                %composer.startReplaceableGroup(2002223207)
                val i = items.next()
                P(i, %composer)
                if (i == 0) {
                  %composer.endReplaceableGroup()
                  break
                }
                %composer.endReplaceableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testBreakWithCallsBeforeAndAfter(): Unit = controlFlow(
        """
            @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    P(i)
                    if (i == 0) {
                        break
                    }
                    P(i)
                }
            }
        """,
        """
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              while (items.hasNext()) {
                %composer.startReplaceableGroup(2002223207)
                val i = items.next()
                P(i, %composer)
                if (i == 0) {
                  %composer.endReplaceableGroup()
                  break
                }
                P(i, %composer)
                %composer.endReplaceableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testContinueWithCallsAfter(): Unit = controlFlow(
        """
            @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    if (i == 0) {
                        continue
                    }
                    P(i)
                }
            }
        """,
        """
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              while (items.hasNext()) {
                %composer.startReplaceableGroup(2002223207)
                val i = items.next()
                if (i == 0) {
                  %composer.endReplaceableGroup()
                  continue
                }
                P(i, %composer)
                %composer.endReplaceableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testContinueWithCallsBefore(): Unit = controlFlow(
        """
            @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    P(i)
                    if (i == 0) {
                        continue
                    }
                    print(i)
                }
            }
        """,
        """
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              while (items.hasNext()) {
                %composer.startReplaceableGroup(2002223207)
                val i = items.next()
                P(i, %composer)
                if (i == 0) {
                  %composer.endReplaceableGroup()
                  continue
                }
                print(i)
                %composer.endReplaceableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testContinueWithCallsBeforeAndAfter(): Unit = controlFlow(
        """
            @Composable
            fun Example(items: Iterator<Int>) {
                while (items.hasNext()) {
                    val i = items.next()
                    P(i)
                    if (i == 0) {
                        continue
                    }
                    P(i)
                }
            }
        """,
        """
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              while (items.hasNext()) {
                %composer.startReplaceableGroup(2002223207)
                val i = items.next()
                P(i, %composer)
                if (i == 0) {
                  %composer.endReplaceableGroup()
                  continue
                }
                P(i, %composer)
                %composer.endReplaceableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testNestedLoopsAndBreak(): Unit = controlFlow(
        """
            @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>) {
                a@while (a.hasNext()) {
                    val x = a.next()
                    if (x == 0) {
                        break
                    }
                    b@while (b.hasNext()) {
                        val y = b.next()
                        if (y == 0) {
                            break
                        }
                        if (y == x) {
                            break@a
                        }
                        if (y == 100) {
                            return
                        }
                        A()
                    }
                    A()
                }
            }
        """,
        """
            @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>, %composer: Composer<N>?) {
              %composer.startReplaceableGroup(2002223143)
              a@while (a.hasNext()) {
                %composer.startReplaceableGroup(2002223219)
                val x = a.next()
                if (x == 0) {
                  %composer.endReplaceableGroup()
                  break
                }
                b@while (b.hasNext()) {
                  %composer.startReplaceableGroup(2002223326)
                  val y = b.next()
                  if (y == 0) {
                    %composer.endReplaceableGroup()
                    break
                  }
                  if (y == x) {
                    %composer.endReplaceableGroup()
                    %composer.endReplaceableGroup()
                    break@a
                  }
                  if (y == 100) {
                    %composer.endReplaceableGroup()
                    %composer.endReplaceableGroup()
                    %composer.endReplaceableGroup()
                    return
                  }
                  A(%composer)
                  %composer.endReplaceableGroup()
                }
                A(%composer)
                %composer.endReplaceableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )
}