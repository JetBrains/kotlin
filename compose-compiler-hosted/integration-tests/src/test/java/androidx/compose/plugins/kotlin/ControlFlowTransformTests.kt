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
        ComposeTransforms.CONTROL_FLOW_GROUPS or
        ComposeTransforms.FUNCTION_BODY_SKIPPING,
        """
            import androidx.compose.Composable
            import androidx.compose.key
            import androidx.compose.Direct

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
            @Direct @Composable
            fun Example(x: Int) {
                // No composable calls, so no group generated except for at function boundary
                if (x > 0) {
                    NA()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              if (greater(x, 0)) {
                NA()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testIfWithCallsInBranch(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                // Composable calls in the result blocks, so we can determine static number of
                // groups executed. This means we put a group around the "then" and the implicit
                // "else" blocks
                if (x > 0) {
                    A()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              if (greater(x, 0)) {
                %composer.startReplaceableGroup(2002223436)
                A(%composer, 0)
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
            @Direct @Composable
            fun Example(x: Int) {
                // Composable calls in the result blocks, so we can determine static number of
                // groups executed. This means we put a group around the "then" and the
                // "else" blocks
                if (x > 0) {
                    A()
                } else {
                    A()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              if (greater(x, 0)) {
                %composer.startReplaceableGroup(2002223427)
                A(%composer, 0)
                %composer.endReplaceableGroup()
              } else {
                %composer.startReplaceableGroup(2002223452)
                A(%composer, 0)
                %composer.endReplaceableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testIfWithCallInCondition(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                // Since the first condition of an if/else is unconditionally executed, it does not
                // necessitate a group of any kind, so we just end up with the function boundary
                // group
                if (B()) {
                    NA()
                } else {
                    NA()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              if (B(%composer, 0)) {
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
            @Direct @Composable
            fun Example(x: Int) {
                // Since the condition in the else-if is conditionally executed, it means we have
                // dynamic execution and we can't statically guarantee the number of groups. As a
                // result, we generate a group around the if statement in addition to a group around
                // each of the conditions with composable calls in them. Note that no group is 
                // needed around the else condition
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
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              if (%composer.startReplaceableGroup(2002223625)
              val tmp0_group = B(%composer, 0)
              %composer.endReplaceableGroup()
              tmp0_group) {
                NA()
              } else if (%composer.startReplaceableGroup(2002223660)
              val tmp1_group = B(%composer, 0)
              %composer.endReplaceableGroup()
              tmp1_group) {
                NA()
              } else {
                NA()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhenWithSubjectAndNoCalls(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                // nothing needed except for the function boundary group
                when (x) {
                    0 -> 8
                    1 -> 10
                    else -> x
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              val tmp0_subject = x
              when {
                EQEQ(tmp0_subject, 0) -> {
                  8
                }
                EQEQ(tmp0_subject, 1) -> {
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
            @Direct @Composable
            fun Example(x: Int) {
                // calls only in the result block, which means we can statically guarantee the
                // number of groups, so no group around the when is needed, just groups around the
                // result blocks.
                when (x) {
                    0 -> A()
                    1 -> A()
                    else -> A()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              val tmp0_subject = x
              when {
                EQEQ(tmp0_subject, 0) -> {
                  %composer.startReplaceableGroup(2002223452)
                  A(%composer, 0)
                  %composer.endReplaceableGroup()
                }
                EQEQ(tmp0_subject, 1) -> {
                  %composer.startReplaceableGroup(2002223469)
                  A(%composer, 0)
                  %composer.endReplaceableGroup()
                }
                else -> {
                  %composer.startReplaceableGroup(2002223489)
                  A(%composer, 0)
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
            @Direct @Composable
            fun Example(x: Int) {
                // no need for a group around the when expression overall, but since the result
                // of the expression is now being used, we need to generate temporary variables to
                // capture the result but still do the execution of the expression inside of groups.
                var y = when (x) {
                    0 -> R()
                    1 -> R()
                    else -> R()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              var y = val tmp0_subject = x
              when {
                EQEQ(tmp0_subject, 0) -> {
                  %composer.startReplaceableGroup(2002223528)
                  val tmp0_group = R(%composer, 0)
                  %composer.endReplaceableGroup()
                  tmp0_group
                }
                EQEQ(tmp0_subject, 1) -> {
                  %composer.startReplaceableGroup(2002223545)
                  val tmp1_group = R(%composer, 0)
                  %composer.endReplaceableGroup()
                  tmp1_group
                }
                else -> {
                  %composer.startReplaceableGroup(2002223565)
                  val tmp2_group = R(%composer, 0)
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
            @Direct @Composable
            fun Example(x: Int) {
                // result blocks have composable calls, so we generate groups round them. It's a
                // statically guaranteed number of groups at execution, so no wrapping group is
                // needed.
                when {
                    x < 0 -> A()
                    x > 30 -> A()
                    else -> A()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              when {
                less(x, 0) -> {
                  %composer.startReplaceableGroup(2002223444)
                  A(%composer, 0)
                  %composer.endReplaceableGroup()
                }
                greater(x, 30) -> {
                  %composer.startReplaceableGroup(2002223466)
                  A(%composer, 0)
                  %composer.endReplaceableGroup()
                }
                else -> {
                  %composer.startReplaceableGroup(2002223486)
                  A(%composer, 0)
                  %composer.endReplaceableGroup()
                }
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhenWithCallsInSomeResults(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                // result blocks have composable calls, so we generate groups round them. It's a
                // statically guaranteed number of groups at execution, so no wrapping group is
                // needed.
                when {
                    x < 0 -> A()
                    x > 30 -> NA()
                    else -> A()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              when {
                less(x, 0) -> {
                  %composer.startReplaceableGroup(2002223444)
                  A(%composer, 0)
                  %composer.endReplaceableGroup()
                }
                greater(x, 30) -> {
                  %composer.startReplaceableGroup(2002223466)
                  %composer.endReplaceableGroup()
                  NA()
                }
                else -> {
                  %composer.startReplaceableGroup(2002223487)
                  A(%composer, 0)
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
            @Direct @Composable
            fun Example(x: Int) {
                // composable calls are in the condition blocks of the when statement. Since these
                // are conditionally executed, we can't statically know the number of groups during
                // execution. as a result, we must wrap the when clause with a group. Since there
                // are no other composable calls, the function body group will suffice.
                when {
                    x == R() -> NA()
                    x > R() -> NA()
                    else -> NA()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              when {
                %composer.startReplaceableGroup(2002223588)
                val tmp0_group = EQEQ(x, R(%composer, 0))
                %composer.endReplaceableGroup()
                tmp0_group -> {
                  NA()
                }
                %composer.startReplaceableGroup(2002223613)
                val tmp1_group = greater(x, R(%composer, 0))
                %composer.endReplaceableGroup()
                tmp1_group -> {
                  NA()
                }
                else -> {
                  NA()
                }
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhenWithCallsInConditionsAndCallAfter(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                // composable calls are in the condition blocks of the when statement. Since these
                // are conditionally executed, we can't statically know the number of groups during
                // execution. as a result, we must wrap the when clause with a group.
                when {
                    x == R() -> NA()
                    x > R() -> NA()
                    else -> NA()
                }
                A()
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              %composer.startReplaceableGroup(2002223485)
              when {
                %composer.startReplaceableGroup(2002223500)
                val tmp0_group = EQEQ(x, R(%composer, 0))
                %composer.endReplaceableGroup()
                tmp0_group -> {
                  NA()
                }
                %composer.startReplaceableGroup(2002223525)
                val tmp1_group = greater(x, R(%composer, 0))
                %composer.endReplaceableGroup()
                tmp1_group -> {
                  NA()
                }
                else -> {
                  NA()
                }
              }
              %composer.endReplaceableGroup()
              A(%composer, 0)
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testSafeCall(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int?) {
                // the composable call is made conditionally, which means it is like an if, but one
                // with static groups, so no wrapping group needed.
                x?.A()
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int?, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              val tmp0_safe_receiver = x
              when {
                EQEQ(tmp0_safe_receiver, null) -> {
                  %composer.startReplaceableGroup(2002223384)
                  %composer.endReplaceableGroup()
                  null
                }
                else -> {
                  %composer.startReplaceableGroup(2002223384)
                  tmp0_safe_receiver.A(%composer, 0)
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
            @Direct @Composable
            fun Example(x: Int?) {
                // the composable call is made conditionally, which means it is like an if, but one
                // with static groups, so no wrapping group needed.
                val y = x ?: R()
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int?, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              val y = val tmp0_elvis_lhs = x
              when {
                EQEQ(tmp0_elvis_lhs, null) -> {
                  %composer.startReplaceableGroup(2002223394)
                  val tmp0_group = R(%composer, 0)
                  %composer.endReplaceableGroup()
                  tmp0_group
                }
                else -> {
                  %composer.startReplaceableGroup(2002223389)
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
            @Direct @Composable
            fun Example(items: List<Int>) {
                // The composable call is made a conditional number of times, so we need to wrap
                // the loop with a dynamic wrapping group. Since there are no other calls, the
                // function body group will suffice.
                for (i in items) {
                    P(i)
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(items: List<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              val tmp0_iterator = items.iterator()
              while (tmp0_iterator.hasNext()) {
                val i = tmp0_iterator.next()
                P(i, %composer, 0)
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testForLoopWithCallsInBodyAndCallsAfter(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(items: List<Int>) {
                // The composable call is made a conditional number of times, so we need to wrap
                // the loop with a dynamic wrapping group.
                for (i in items) {
                    P(i)
                }
                A()
            }
        """,
        """
            @Direct
            @Composable
            fun Example(items: List<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              val tmp0_iterator = items.iterator()
              %composer.startReplaceableGroup(2002223378)
              while (tmp0_iterator.hasNext()) {
                val i = tmp0_iterator.next()
                P(i, %composer, 0)
              }
              %composer.endReplaceableGroup()
              A(%composer, 0)
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testForLoopWithCallsInSubject(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example() {
                // The for loop's subject expression is only executed once, so we don't need any
                // additional groups
                for (i in L()) {
                    print(i)
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(%composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              val tmp0_iterator = L(%composer, 0).iterator()
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
            @Direct @Composable
            fun Example(items: MutableList<Int>) {
                // since we have a composable call which is called a conditional number of times,
                // we need to generate groups around the loop's block as well as a group around the
                // overall statement. Since there are no calls after the while loop, the function
                // body group will suffice.
                while (items.isNotEmpty()) {
                    val item = items.removeAt(items.size - 1)
                    P(item)
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(items: MutableList<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (items.isNotEmpty()) {
                val item = items.removeAt(items.size - 1)
                P(item, %composer, 0)
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInBodyAndCallsAfter(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(items: MutableList<Int>) {
                // since we have a composable call which is called a conditional number of times,
                // we need to generate groups around the loop's block as well as a group around the
                // overall statement.
                while (items.isNotEmpty()) {
                    val item = items.removeAt(items.size - 1)
                    P(item)
                }
                A()
            }
        """,
        """
            @Direct
            @Composable
            fun Example(items: MutableList<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              %composer.startReplaceableGroup(2002223453)
              while (items.isNotEmpty()) {
                val item = items.removeAt(items.size - 1)
                P(item, %composer, 0)
              }
              %composer.endReplaceableGroup()
              A(%composer, 0)
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInCondition(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example() {
                // A while loop's condition block gets executed a conditional number of times, so
                // so we must generate a group around the while expression overall. The function
                // body group will suffice.
                while (B()) {
                    print("hello world")
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(%composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (B(%composer, 0)) {
                print("hello world")
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInConditionAndCallsAfter(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example() {
                // A while loop's condition block gets executed a conditional number of times, so
                // so we must generate a group around the while expression overall.
                while (B()) {
                    print("hello world")
                }
                A()
            }
        """,
        """
            @Direct
            @Composable
            fun Example(%composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              %composer.startReplaceableGroup(2002223388)
              while (B(%composer, 0)) {
                print("hello world")
              }
              %composer.endReplaceableGroup()
              A(%composer, 0)
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInConditionAndBody(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example() {
                // Both the condition and the body of the loop get groups because they have
                // composable calls in them. We must generate a group around the while statement
                // overall, but the function body group will suffice.
                while (B()) {
                    A()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(%composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (B(%composer, 0)) {
                A(%composer, 0)
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileLoopWithCallsInConditionAndBodyAndCallsAfter(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example() {
                // Both the condition and the body of the loop get groups because they have
                // composable calls in them. We must generate a group around the while statement
                // overall.
                while (B()) {
                    A()
                }
                A()
            }
        """,
        """
            @Direct
            @Composable
            fun Example(%composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              %composer.startReplaceableGroup(2002223411)
              while (B(%composer, 0)) {
                A(%composer, 0)
              }
              %composer.endReplaceableGroup()
              A(%composer, 0)
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testEarlyReturnWithCallsBeforeButNotAfter(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                // in the early return path, we need only close out the opened groups
                if (x > 0) {
                    A()
                    return
                }
                print("hello")
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              if (greater(x, 0)) {
                %composer.startReplaceableGroup(2002223321)
                A(%composer, 0)
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
            @Direct @Composable
            fun Example(x: Int) {
                // we can just close out the open groups at the return.
                if (x > 0) {
                    return
                }
                A()
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              if (greater(x, 0)) {
                %composer.endReplaceableGroup()
                return
              }
              A(%composer, 0)
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testEarlyReturnValue(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int): Int {
                if (x > 0) {
                    A()
                    return 1
                }
                return 2
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int): Int {
              %composer.startReplaceableGroup(2002223210)
              if (greater(x, 0)) {
                %composer.startReplaceableGroup(2002223252)
                A(%composer, 0)
                val tmp1_return = 1
                %composer.endReplaceableGroup()
                %composer.endReplaceableGroup()
                return tmp1_return
              } else {
                %composer.startReplaceableGroup(2002223093)
                %composer.endReplaceableGroup()
              }
              val tmp0 = 2
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testEarlyReturnValueWithCallsAfter(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int): Int {
                if (x > 0) {
                    return 1
                }
                A()
                return 2
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int): Int {
              %composer.startReplaceableGroup(2002223210)
              if (greater(x, 0)) {
                val tmp1_return = 1
                %composer.endReplaceableGroup()
                return tmp1_return
              }
              A(%composer, 0)
              val tmp0 = 2
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testReturnCallValue(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(): Int {
                // since the return expression is a composable call, we need to generate a
                // temporary variable and then return it after ending the open groups.
                A()
                return R()
            }
        """,
        """
            @Direct
            @Composable
            fun Example(%composer: Composer<*>?, %changed: Int): Int {
              %composer.startReplaceableGroup(2002223210)
              A(%composer, 0)
              val tmp0 = R(%composer, 0)
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testEarlyReturnCallValue(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int): Int {
                if (x > 0) {
                    return R()
                }
                return R()
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int): Int {
              %composer.startReplaceableGroup(2002223210)
              if (greater(x, 0)) {
                %composer.startReplaceableGroup(2002223252)
                val tmp1_return = R(%composer, 0)
                %composer.endReplaceableGroup()
                %composer.endReplaceableGroup()
                return tmp1_return
              } else {
                %composer.startReplaceableGroup(2002223093)
                %composer.endReplaceableGroup()
              }
              val tmp0 = R(%composer, 0)
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testReturnFromLoop(): Unit = controlFlow(
        """
            @Direct @Composable
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
            @Direct
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (items.hasNext()) {
                val i = items.next()
                P(i, %composer, 0)
                if (EQEQ(i, 0)) {
                  %composer.startReplaceableGroup(2002223338)
                  P(i, %composer, 0)
                  %composer.endReplaceableGroup()
                  %composer.endReplaceableGroup()
                  return
                } else {
                  %composer.startReplaceableGroup(2002223391)
                  P(i, %composer, 0)
                  %composer.endReplaceableGroup()
                }
                P(i, %composer, 0)
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testOrderingOfPushedEndCallsWithEarlyReturns(): Unit = controlFlow(
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
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startRestartGroup(2002223202)
              while (items.hasNext()) {
                val i = items.next()
                P(i, %composer, 0)
                if (EQEQ(i, 0)) {
                  %composer.startReplaceableGroup(2002223330)
                  P(i, %composer, 0)
                  %composer.endReplaceableGroup()
                  %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                    Example(items, %composer, %changed or 0b0001)
                  }
                  return
                } else {
                  %composer.startReplaceableGroup(2002223383)
                  P(i, %composer, 0)
                  %composer.endReplaceableGroup()
                }
                P(i, %composer, 0)
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<N>? ->
                Example(items, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testBreakWithCallsAfter(): Unit = controlFlow(
            """
            @Direct @Composable
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
            @Direct
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (items.hasNext()) {
                val i = items.next()
                if (EQEQ(i, 0)) {
                  break
                }
                P(i, %composer, 0)
              }
              %composer.endReplaceableGroup()
            }
        """
        )

    @Test
    fun testBreakWithCallsBefore(): Unit = controlFlow(
        """
            @Direct @Composable
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
            @Direct
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (items.hasNext()) {
                val i = items.next()
                P(i, %composer, 0)
                if (EQEQ(i, 0)) {
                  break
                }
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testBreakWithCallsBeforeAndAfter(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(items: Iterator<Int>) {
                // a group around while is needed here, but the function body group will suffice
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
            @Direct
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (items.hasNext()) {
                val i = items.next()
                P(i, %composer, 0)
                if (EQEQ(i, 0)) {
                  break
                }
                P(i, %composer, 0)
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testBreakWithCallsBeforeAndAfterAndCallAfter(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(items: Iterator<Int>) {
                // a group around while is needed here
                while (items.hasNext()) {
                    val i = items.next()
                    P(i)
                    if (i == 0) {
                        break
                    }
                    P(i)
                }
                A()
            }
        """,
        """
            @Direct
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              %composer.startReplaceableGroup(2002223293)
              while (items.hasNext()) {
                val i = items.next()
                P(i, %composer, 0)
                if (EQEQ(i, 0)) {
                  break
                }
                P(i, %composer, 0)
              }
              %composer.endReplaceableGroup()
              A(%composer, 0)
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testContinueWithCallsAfter(): Unit = controlFlow(
        """
            @Direct @Composable
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
            @Direct
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (items.hasNext()) {
                val i = items.next()
                if (EQEQ(i, 0)) {
                  continue
                }
                P(i, %composer, 0)
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testContinueWithCallsBefore(): Unit = controlFlow(
        """
            @Direct @Composable
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
            @Direct
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (items.hasNext()) {
                val i = items.next()
                P(i, %composer, 0)
                if (EQEQ(i, 0)) {
                  continue
                }
                print(i)
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testContinueWithCallsBeforeAndAfter(): Unit = controlFlow(
        """
            @Direct @Composable
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
            @Direct
            @Composable
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (items.hasNext()) {
                val i = items.next()
                P(i, %composer, 0)
                if (EQEQ(i, 0)) {
                  continue
                }
                P(i, %composer, 0)
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testLoopWithReturn(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>) {
                while (a.hasNext()) {
                    val x = a.next()
                    if (x > 100) {
                        return
                    }
                    A()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (a.hasNext()) {
                val x = a.next()
                if (greater(x, 100)) {
                  %composer.endReplaceableGroup()
                  return
                }
                A(%composer, 0)
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testLoopWithBreak(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>) {
                a@while (a.hasNext()) {
                    val x = a.next()
                    b@while (b.hasNext()) {
                        val y = b.next()
                        if (y == x) {
                            break@a
                        }
                        A()
                    }
                    A()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              a@while (a.hasNext()) {
                val x = a.next()
                %composer.startReplaceableGroup(2002223323)
                b@while (b.hasNext()) {
                  val y = b.next()
                  if (EQEQ(y, x)) {
                    %composer.endReplaceableGroup()
                    break@a
                  }
                  A(%composer, 0)
                }
                %composer.endReplaceableGroup()
                A(%composer, 0)
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testNestedLoopsAndBreak(): Unit = controlFlow(
        """
            @Direct @Composable
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
                        if (y > 100) {
                            return
                        }
                        A()
                    }
                    A()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              a@while (a.hasNext()) {
                val x = a.next()
                if (EQEQ(x, 0)) {
                  break
                }
                %composer.startReplaceableGroup(2002223373)
                b@while (b.hasNext()) {
                  val y = b.next()
                  if (EQEQ(y, 0)) {
                    break
                  }
                  if (EQEQ(y, x)) {
                    %composer.endReplaceableGroup()
                    break@a
                  }
                  if (greater(y, 100)) {
                    %composer.endReplaceableGroup()
                    %composer.endReplaceableGroup()
                    return
                  }
                  A(%composer, 0)
                }
                %composer.endReplaceableGroup()
                A(%composer, 0)
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testNestedLoops(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>) {
                a@while (a.hasNext()) {
                    b@while (b.hasNext()) {
                        A()
                    }
                    A()
                }
                A()
            }
        """,
        """
            @Direct
            @Composable
            fun Example(a: Iterator<Int>, b: Iterator<Int>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              %composer.startReplaceableGroup(2002223266)
              a@while (a.hasNext()) {
                %composer.startReplaceableGroup(2002223298)
                b@while (b.hasNext()) {
                  A(%composer, 0)
                }
                %composer.endReplaceableGroup()
                A(%composer, 0)
              }
              %composer.endReplaceableGroup()
              A(%composer, 0)
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileInsideIfAndCallAfter(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    while (x > 0) {
                        A()
                    }
                    A()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              if (greater(x, 0)) {
                %composer.startReplaceableGroup(2002223247)
                %composer.startReplaceableGroup(2002223257)
                while (greater(x, 0)) {
                  A(%composer, 0)
                }
                %composer.endReplaceableGroup()
                A(%composer, 0)
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
    fun testWhileInsideIfAndCallBefore(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    A()
                    while (x > 0) {
                        A()
                    }
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              if (greater(x, 0)) {
                %composer.startReplaceableGroup(2002223247)
                A(%composer, 0)
                while (greater(x, 0)) {
                  A(%composer, 0)
                }
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
    fun testWhileInsideIf(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    while (x > 0) {
                        A()
                    }
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              if (greater(x, 0)) {
                %composer.startReplaceableGroup(2002223247)
                while (greater(x, 0)) {
                  A(%composer, 0)
                }
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
    fun testWhileWithKey(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                while (x > 0) {
                    key(x) {
                        A()
                    }
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (greater(x, 0)) {
                %composer.startMovableGroup(%composer.joinKey(2002223260, x))
                A(%composer, 0)
                %composer.endMovableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileWithTwoKeys(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                while (x > 0) {
                    key(x) {
                        A()
                    }
                    key(x+1) {
                        A()
                    }
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (greater(x, 0)) {
                %composer.startMovableGroup(%composer.joinKey(2002223260, x))
                A(%composer, 0)
                %composer.endMovableGroup()
                %composer.startMovableGroup(%composer.joinKey(2002223303, x + 1))
                A(%composer, 0)
                %composer.endMovableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileWithKeyAndCallAfter(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                while (x > 0) {
                    key(x) {
                        A()
                    }
                    A()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (greater(x, 0)) {
                %composer.startMovableGroup(%composer.joinKey(2002223260, x))
                A(%composer, 0)
                %composer.endMovableGroup()
                A(%composer, 0)
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileWithKeyAndCallBefore(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                while (x > 0) {
                    A()
                    key(x) {
                        A()
                    }
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (greater(x, 0)) {
                A(%composer, 0)
                %composer.startMovableGroup(%composer.joinKey(2002223272, x))
                A(%composer, 0)
                %composer.endMovableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testWhileWithKeyAndCallBeforeAndAfter(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                while (x > 0) {
                    A()
                    key(x) {
                        A()
                    }
                    A()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (greater(x, 0)) {
                A(%composer, 0)
                %composer.startMovableGroup(%composer.joinKey(2002223272, x))
                A(%composer, 0)
                %composer.endMovableGroup()
                A(%composer, 0)
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testKeyAtRootLevel(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                key(x) {
                    A()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              %composer.startMovableGroup(%composer.joinKey(2002223236, x))
              A(%composer, 0)
              %composer.endMovableGroup()
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testKeyAtRootLevelAndCallsAfter(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                key(x) {
                    A()
                }
                A()
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              %composer.startMovableGroup(%composer.joinKey(2002223236, x))
              A(%composer, 0)
              %composer.endMovableGroup()
              A(%composer, 0)
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testKeyAtRootLevelAndCallsBefore(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                A()
                key(x) {
                    A()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              A(%composer, 0)
              %composer.startMovableGroup(%composer.joinKey(2002223244, x))
              A(%composer, 0)
              %composer.endMovableGroup()
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testKeyInIf(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    key(x) {
                        A()
                    }
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              if (greater(x, 0)) {
                %composer.startReplaceableGroup(2002223247)
                %composer.startMovableGroup(%composer.joinKey(2002223257, x))
                A(%composer, 0)
                %composer.endMovableGroup()
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
    fun testKeyInIfAndCallsAfter(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    key(x) {
                        A()
                    }
                    A()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              if (greater(x, 0)) {
                %composer.startReplaceableGroup(2002223247)
                %composer.startMovableGroup(%composer.joinKey(2002223257, x))
                A(%composer, 0)
                %composer.endMovableGroup()
                A(%composer, 0)
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
    fun testKeyInIfAndCallsBefore(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                if (x > 0) {
                    A()
                    key(x) {
                        A()
                    }
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              if (greater(x, 0)) {
                %composer.startReplaceableGroup(2002223247)
                A(%composer, 0)
                %composer.startMovableGroup(%composer.joinKey(2002223269, x))
                A(%composer, 0)
                %composer.endMovableGroup()
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
    fun testKeyWithLotsOfValues(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(a: Int, b: Int, c: Int, d: Int) {
                key(a, b, c, d) {
                    A()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(a: Int, b: Int, c: Int, d: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              %composer.startMovableGroup(%composer.joinKey(%composer.joinKey(%composer.joinKey(%composer.joinKey(2002223260, a), b), c), d))
              A(%composer, 0)
              %composer.endMovableGroup()
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testKeyWithComposableValue(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                while(x > 0) {
                    key(R()) {
                        A()
                    }
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              while (greater(x, 0)) {
                %composer.startMovableGroup(%composer.joinKey(2002223259, R(%composer, 0)))
                A(%composer, 0)
                %composer.endMovableGroup()
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testKeyAsAValue(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int) {
                val y = key(x) { R() }
                P(y)
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(2002223210)
              val y =
              %composer.startMovableGroup(%composer.joinKey(2002223244, x))
              val tmp0 = R(%composer, 0)
              %composer.endMovableGroup()
              tmp0
              P(y, %composer, 0)
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testDynamicWrappingGroupWithReturnValue(): Unit = controlFlow(
        """
            @Direct @Composable
            fun Example(x: Int): Int {
                return if (x > 0) {
                    if (B()) 1
                    else if (B()) 2
                    else 3
                } else 4
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %changed: Int): Int {
              %composer.startReplaceableGroup(2002223210)
              val tmp0 = if (greater(x, 0)) {
                %composer.startReplaceableGroup(2002223259)
                val tmp3_group =
                if (%composer.startReplaceableGroup(2002223273)
                val tmp1_group = B(%composer, 0)
                %composer.endReplaceableGroup()
                tmp1_group) 1 else if (%composer.startReplaceableGroup(2002223297)
                val tmp2_group = B(%composer, 0)
                %composer.endReplaceableGroup()
                tmp2_group) 2 else 3
                %composer.endReplaceableGroup()
                tmp3_group
              } else {
                %composer.startReplaceableGroup(2002223330)
                %composer.endReplaceableGroup()
                4
              }
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )
}