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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              if (x > 0) {
                %composer.startReplaceableGroup(<>)
                A(%composer, <>, 0)
                %composer.endReplaceableGroup()
              } else {
                %composer.startReplaceableGroup(<>)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              if (x > 0) {
                %composer.startReplaceableGroup(<>)
                A(%composer, <>, 0)
                %composer.endReplaceableGroup()
              } else {
                %composer.startReplaceableGroup(<>)
                A(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              if (B(%composer, <>, 0)) {
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              if (%composer.startReplaceableGroup(<>)
              val tmp0_group = B(%composer, <>, 0)
              %composer.endReplaceableGroup()
              tmp0_group) {
                NA()
              } else if (%composer.startReplaceableGroup(<>)
              val tmp1_group = B(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              val tmp0_subject = x
              when {
                tmp0_subject == 0 -> {
                  8
                }
                tmp0_subject == 0b0001 -> {
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              val tmp0_subject = x
              when {
                tmp0_subject == 0 -> {
                  %composer.startReplaceableGroup(<>)
                  A(%composer, <>, 0)
                  %composer.endReplaceableGroup()
                }
                tmp0_subject == 0b0001 -> {
                  %composer.startReplaceableGroup(<>)
                  A(%composer, <>, 0)
                  %composer.endReplaceableGroup()
                }
                else -> {
                  %composer.startReplaceableGroup(<>)
                  A(%composer, <>, 0)
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
                val y = when (x) {
                    0 -> R()
                    1 -> R()
                    else -> R()
                }
            }
        """,
        """
            @Direct
            @Composable
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              val y = val tmp0_subject = x
              when {
                tmp0_subject == 0 -> {
                  %composer.startReplaceableGroup(<>)
                  val tmp0_group = R(%composer, <>, 0)
                  %composer.endReplaceableGroup()
                  tmp0_group
                }
                tmp0_subject == 0b0001 -> {
                  %composer.startReplaceableGroup(<>)
                  val tmp1_group = R(%composer, <>, 0)
                  %composer.endReplaceableGroup()
                  tmp1_group
                }
                else -> {
                  %composer.startReplaceableGroup(<>)
                  val tmp2_group = R(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              when {
                x < 0 -> {
                  %composer.startReplaceableGroup(<>)
                  A(%composer, <>, 0)
                  %composer.endReplaceableGroup()
                }
                x > 30 -> {
                  %composer.startReplaceableGroup(<>)
                  A(%composer, <>, 0)
                  %composer.endReplaceableGroup()
                }
                else -> {
                  %composer.startReplaceableGroup(<>)
                  A(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              when {
                x < 0 -> {
                  %composer.startReplaceableGroup(<>)
                  A(%composer, <>, 0)
                  %composer.endReplaceableGroup()
                }
                x > 30 -> {
                  %composer.startReplaceableGroup(<>)
                  %composer.endReplaceableGroup()
                  NA()
                }
                else -> {
                  %composer.startReplaceableGroup(<>)
                  A(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              when {
                %composer.startReplaceableGroup(<>)
                val tmp0_group = x == R(%composer, 0b01110111010101111000000111101001, 0)
                %composer.endReplaceableGroup()
                tmp0_group -> {
                  NA()
                }
                %composer.startReplaceableGroup(<>)
                val tmp1_group = x > R(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              %composer.startReplaceableGroup(<>)
              when {
                %composer.startReplaceableGroup(<>)
                val tmp0_group = x == R(%composer, 0b01110111010101111000000110010001, 0)
                %composer.endReplaceableGroup()
                tmp0_group -> {
                  NA()
                }
                %composer.startReplaceableGroup(<>)
                val tmp1_group = x > R(%composer, <>, 0)
                %composer.endReplaceableGroup()
                tmp1_group -> {
                  NA()
                }
                else -> {
                  NA()
                }
              }
              %composer.endReplaceableGroup()
              A(%composer, <>, 0)
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
            fun Example(x: Int?, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              val tmp0_safe_receiver = x
              when {
                tmp0_safe_receiver == null -> {
                  %composer.startReplaceableGroup(<>)
                  %composer.endReplaceableGroup()
                  null
                }
                else -> {
                  %composer.startReplaceableGroup(<>)
                  tmp0_safe_receiver.A(%composer, <>, 0b0110 and %changed)
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
            fun Example(x: Int?, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              val y = val tmp0_elvis_lhs = x
              when {
                tmp0_elvis_lhs == null -> {
                  %composer.startReplaceableGroup(<>)
                  val tmp0_group = R(%composer, <>, 0)
                  %composer.endReplaceableGroup()
                  tmp0_group
                }
                else -> {
                  %composer.startReplaceableGroup(<>)
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
            fun Example(items: List<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              val tmp0_iterator = items.iterator()
              while (tmp0_iterator.hasNext()) {
                val i = tmp0_iterator.next()
                P(i, %composer, <>, 0)
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
            fun Example(items: List<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              %composer.startReplaceableGroup(<>)
              val tmp0_iterator = items.iterator()
              while (tmp0_iterator.hasNext()) {
                val i = tmp0_iterator.next()
                P(i, %composer, <>, 0)
              }
              %composer.endReplaceableGroup()
              A(%composer, <>, 0)
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
            fun Example(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              val tmp0_iterator = L(%composer, <>, 0).iterator()
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
            fun Example(items: MutableList<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (items.isNotEmpty()) {
                val item = items.removeAt(items.size - 1)
                P(item, %composer, <>, 0)
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
            fun Example(items: MutableList<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              %composer.startReplaceableGroup(<>)
              while (items.isNotEmpty()) {
                val item = items.removeAt(items.size - 1)
                P(item, %composer, <>, 0)
              }
              %composer.endReplaceableGroup()
              A(%composer, <>, 0)
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
            fun Example(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (B(%composer, <>, 0)) {
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
            fun Example(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              %composer.startReplaceableGroup(<>)
              while (B(%composer, <>, 0)) {
                print("hello world")
              }
              %composer.endReplaceableGroup()
              A(%composer, <>, 0)
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
            fun Example(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (B(%composer, <>, 0)) {
                A(%composer, <>, 0)
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
            fun Example(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              %composer.startReplaceableGroup(<>)
              while (B(%composer, <>, 0)) {
                A(%composer, <>, 0)
              }
              %composer.endReplaceableGroup()
              A(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              if (x > 0) {
                %composer.startReplaceableGroup(<>)
                A(%composer, <>, 0)
                %composer.endReplaceableGroup()
                %composer.endReplaceableGroup()
                return
              } else {
                %composer.startReplaceableGroup(<>)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              if (x > 0) {
                %composer.endReplaceableGroup()
                return
              }
              A(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int): Int {
              %composer.startReplaceableGroup(%key)
              if (x > 0) {
                %composer.startReplaceableGroup(<>)
                A(%composer, <>, 0)
                val tmp1_return = 1
                %composer.endReplaceableGroup()
                %composer.endReplaceableGroup()
                return tmp1_return
              } else {
                %composer.startReplaceableGroup(<>)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int): Int {
              %composer.startReplaceableGroup(%key)
              if (x > 0) {
                val tmp1_return = 1
                %composer.endReplaceableGroup()
                return tmp1_return
              }
              A(%composer, <>, 0)
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
            fun Example(%composer: Composer<*>?, %key: Int, %changed: Int): Int {
              %composer.startReplaceableGroup(%key)
              A(%composer, <>, 0)
              val tmp0 = R(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int): Int {
              %composer.startReplaceableGroup(%key)
              if (x > 0) {
                %composer.startReplaceableGroup(<>)
                val tmp1_return = R(%composer, <>, 0)
                %composer.endReplaceableGroup()
                %composer.endReplaceableGroup()
                return tmp1_return
              } else {
                %composer.startReplaceableGroup(<>)
                %composer.endReplaceableGroup()
              }
              val tmp0 = R(%composer, <>, 0)
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
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (items.hasNext()) {
                val i = items.next()
                P(i, %composer, <>, 0)
                if (i == 0) {
                  %composer.startReplaceableGroup(<>)
                  P(i, %composer, <>, 0)
                  %composer.endReplaceableGroup()
                  %composer.endReplaceableGroup()
                  return
                } else {
                  %composer.startReplaceableGroup(<>)
                  P(i, %composer, <>, 0)
                  %composer.endReplaceableGroup()
                }
                P(i, %composer, <>, 0)
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
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startRestartGroup(%key)
              while (items.hasNext()) {
                val i = items.next()
                P(i, %composer, <>, 0)
                if (i == 0) {
                  %composer.startReplaceableGroup(<>)
                  P(i, %composer, <>, 0)
                  %composer.endReplaceableGroup()
                  %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %key: Int, %force: Int ->
                    Example(items, %composer, %key, %changed or 0b0001)
                  }
                  return
                } else {
                  %composer.startReplaceableGroup(<>)
                  P(i, %composer, <>, 0)
                  %composer.endReplaceableGroup()
                }
                P(i, %composer, <>, 0)
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %key: Int, %force: Int ->
                Example(items, %composer, %key, %changed or 0b0001)
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
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (items.hasNext()) {
                val i = items.next()
                if (i == 0) {
                  break
                }
                P(i, %composer, <>, 0)
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
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (items.hasNext()) {
                val i = items.next()
                P(i, %composer, <>, 0)
                if (i == 0) {
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
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (items.hasNext()) {
                val i = items.next()
                P(i, %composer, <>, 0)
                if (i == 0) {
                  break
                }
                P(i, %composer, <>, 0)
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
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              %composer.startReplaceableGroup(<>)
              while (items.hasNext()) {
                val i = items.next()
                P(i, %composer, <>, 0)
                if (i == 0) {
                  break
                }
                P(i, %composer, <>, 0)
              }
              %composer.endReplaceableGroup()
              A(%composer, <>, 0)
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
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (items.hasNext()) {
                val i = items.next()
                if (i == 0) {
                  continue
                }
                P(i, %composer, <>, 0)
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
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (items.hasNext()) {
                val i = items.next()
                P(i, %composer, <>, 0)
                if (i == 0) {
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
            fun Example(items: Iterator<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (items.hasNext()) {
                val i = items.next()
                P(i, %composer, <>, 0)
                if (i == 0) {
                  continue
                }
                P(i, %composer, <>, 0)
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
            fun Example(a: Iterator<Int>, b: Iterator<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (a.hasNext()) {
                val x = a.next()
                if (x > 100) {
                  %composer.endReplaceableGroup()
                  return
                }
                A(%composer, <>, 0)
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
            fun Example(a: Iterator<Int>, b: Iterator<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              a@while (a.hasNext()) {
                val x = a.next()
                %composer.startReplaceableGroup(<>)
                b@while (b.hasNext()) {
                  val y = b.next()
                  if (y == x) {
                    %composer.endReplaceableGroup()
                    break@a
                  }
                  A(%composer, <>, 0)
                }
                %composer.endReplaceableGroup()
                A(%composer, <>, 0)
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
            fun Example(a: Iterator<Int>, b: Iterator<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              a@while (a.hasNext()) {
                val x = a.next()
                if (x == 0) {
                  break
                }
                %composer.startReplaceableGroup(<>)
                b@while (b.hasNext()) {
                  val y = b.next()
                  if (y == 0) {
                    break
                  }
                  if (y == x) {
                    %composer.endReplaceableGroup()
                    break@a
                  }
                  if (y > 100) {
                    %composer.endReplaceableGroup()
                    %composer.endReplaceableGroup()
                    return
                  }
                  A(%composer, <>, 0)
                }
                %composer.endReplaceableGroup()
                A(%composer, <>, 0)
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
            fun Example(a: Iterator<Int>, b: Iterator<Int>, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              %composer.startReplaceableGroup(<>)
              a@while (a.hasNext()) {
                %composer.startReplaceableGroup(<>)
                b@while (b.hasNext()) {
                  A(%composer, <>, 0)
                }
                %composer.endReplaceableGroup()
                A(%composer, <>, 0)
              }
              %composer.endReplaceableGroup()
              A(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              if (x > 0) {
                %composer.startReplaceableGroup(<>)
                %composer.startReplaceableGroup(<>)
                while (x > 0) {
                  A(%composer, <>, 0)
                }
                %composer.endReplaceableGroup()
                A(%composer, <>, 0)
                %composer.endReplaceableGroup()
              } else {
                %composer.startReplaceableGroup(<>)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              if (x > 0) {
                %composer.startReplaceableGroup(<>)
                A(%composer, <>, 0)
                while (x > 0) {
                  A(%composer, <>, 0)
                }
                %composer.endReplaceableGroup()
              } else {
                %composer.startReplaceableGroup(<>)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              if (x > 0) {
                %composer.startReplaceableGroup(<>)
                while (x > 0) {
                  A(%composer, <>, 0)
                }
                %composer.endReplaceableGroup()
              } else {
                %composer.startReplaceableGroup(<>)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (x > 0) {
                %composer.startMovableGroup(<>, x)
                A(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (x > 0) {
                %composer.startMovableGroup(<>, x)
                A(%composer, <>, 0)
                %composer.endMovableGroup()
                %composer.startMovableGroup(<>, x + 1)
                A(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (x > 0) {
                %composer.startMovableGroup(<>, x)
                A(%composer, <>, 0)
                %composer.endMovableGroup()
                A(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (x > 0) {
                A(%composer, <>, 0)
                %composer.startMovableGroup(<>, x)
                A(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (x > 0) {
                A(%composer, <>, 0)
                %composer.startMovableGroup(<>, x)
                A(%composer, <>, 0)
                %composer.endMovableGroup()
                A(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              %composer.startMovableGroup(<>, x)
              A(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              %composer.startMovableGroup(<>, x)
              A(%composer, <>, 0)
              %composer.endMovableGroup()
              A(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              A(%composer, <>, 0)
              %composer.startMovableGroup(<>, x)
              A(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              if (x > 0) {
                %composer.startReplaceableGroup(<>)
                %composer.startMovableGroup(<>, x)
                A(%composer, <>, 0)
                %composer.endMovableGroup()
                %composer.endReplaceableGroup()
              } else {
                %composer.startReplaceableGroup(<>)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              if (x > 0) {
                %composer.startReplaceableGroup(<>)
                %composer.startMovableGroup(<>, x)
                A(%composer, <>, 0)
                %composer.endMovableGroup()
                A(%composer, <>, 0)
                %composer.endReplaceableGroup()
              } else {
                %composer.startReplaceableGroup(<>)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              if (x > 0) {
                %composer.startReplaceableGroup(<>)
                A(%composer, <>, 0)
                %composer.startMovableGroup(<>, x)
                A(%composer, <>, 0)
                %composer.endMovableGroup()
                %composer.endReplaceableGroup()
              } else {
                %composer.startReplaceableGroup(<>)
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
            fun Example(a: Int, b: Int, c: Int, d: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              %composer.startMovableGroup(<>, %composer.joinKey(%composer.joinKey(%composer.joinKey(a, b), c), d))
              A(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              while (x > 0) {
                %composer.startMovableGroup(<>, R(%composer, <>, 0))
                A(%composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              val y =
              %composer.startMovableGroup(<>, x)
              val tmp0 = R(%composer, <>, 0)
              %composer.endMovableGroup()
              tmp0
              P(y, %composer, <>, 0)
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
            fun Example(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int): Int {
              %composer.startReplaceableGroup(%key)
              val tmp0 = if (x > 0) {
                %composer.startReplaceableGroup(<>)
                val tmp4_group =
                val tmp3_group = if (%composer.startReplaceableGroup(<>)
                val tmp1_group = B(%composer, <>, 0)
                %composer.endReplaceableGroup()
                tmp1_group) 1 else if (%composer.startReplaceableGroup(<>)
                val tmp2_group = B(%composer, <>, 0)
                %composer.endReplaceableGroup()
                tmp2_group) 2 else 3
                tmp3_group
                %composer.endReplaceableGroup()
                tmp4_group
              } else {
                %composer.startReplaceableGroup(<>)
                %composer.endReplaceableGroup()
                4
              }
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testTheThing(): Unit = controlFlow(
        """
            @Direct
            @Composable
            fun Simple() {
              // this has a composable call in it, and since we don't know the number of times the
              // lambda will get called, we place a group around the whole call
              run {
                A()
              }
              A()
            }

            @Direct
            @Composable
            fun WithReturn() {
              // this has an early return in it, so it needs to end all of the groups present.
              run {
                A()
                return@WithReturn
              }
              A()
            }

            @Direct
            @Composable
            fun NoCalls() {
              // this has no composable calls in it, so shouldn't cause any groups to get created
              run {
                println("hello world")
              }
              A()
            }

            @Direct
            @Composable
            fun NoCallsAfter() {
              // this has a composable call in the lambda, but not after it, which means the
              // group should be able to be coalesced into the group of the function
              run {
                A()
              }
            }
        """,
        """
            @Direct
            @Composable
            fun Simple(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              %composer.startReplaceableGroup(<>)
              run {
                A(%composer, <>, 0)
              }
              %composer.endReplaceableGroup()
              A(%composer, <>, 0)
              %composer.endReplaceableGroup()
            }
            @Direct
            @Composable
            fun WithReturn(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              %composer.startReplaceableGroup(<>)
              run {
                A(%composer, <>, 0)
                %composer.endReplaceableGroup()
                %composer.endReplaceableGroup()
                return
              }
              %composer.endReplaceableGroup()
              A(%composer, <>, 0)
              %composer.endReplaceableGroup()
            }
            @Direct
            @Composable
            fun NoCalls(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              run {
                println("hello world")
              }
              A(%composer, <>, 0)
              %composer.endReplaceableGroup()
            }
            @Direct
            @Composable
            fun NoCallsAfter(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startReplaceableGroup(%key)
              run {
                A(%composer, <>, 0)
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testLetWithComposableCalls(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int?) {
              x?.let {
                if (it > 0) {
                  A()
                }
                A()
              }
              A()
            }
        """,
        """
            @Composable
            fun Example(x: Int?, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startRestartGroup(%key)
              val %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b0011 xor 0b0010 !== 0 || !%composer.skipping) {
                val tmp0_safe_receiver = x
                when {
                  tmp0_safe_receiver == null -> {
                    %composer.startReplaceableGroup(<>)
                    %composer.endReplaceableGroup()
                    null
                  }
                  else -> {
                    %composer.startReplaceableGroup(<>)
                    tmp0_safe_receiver.let { it: Int ->
                      if (it > 0) {
                        %composer.startReplaceableGroup(<>)
                        A(%composer, <>, 0)
                        %composer.endReplaceableGroup()
                      } else {
                        %composer.startReplaceableGroup(<>)
                        %composer.endReplaceableGroup()
                      }
                      A(%composer, <>, 0)
                    }
                    %composer.endReplaceableGroup()
                  }
                }
                A(%composer, <>, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %key: Int, %force: Int ->
                Example(x, %composer, %key, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testLetWithoutComposableCalls(): Unit = controlFlow(
        """
            @Composable
            fun Example(x: Int?) {
              x?.let {
                if (it > 0) {
                  NA()
                }
                NA()
              }
              A()
            }
        """,
        """
            @Composable
            fun Example(x: Int?, %composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startRestartGroup(%key)
              val %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b0011 xor 0b0010 !== 0 || !%composer.skipping) {
                x?.let { it: Int ->
                  if (it > 0) {
                    NA()
                  }
                  NA()
                }
                A(%composer, <>, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %key: Int, %force: Int ->
                Example(x, %composer, %key, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testApplyOnComposableCallResult(): Unit = controlFlow(
        """
            import androidx.compose.state
            import androidx.compose.State

            @Composable
            fun <T> provided(value: T): State<T> = state { value }.apply {
                this.value = value
            }
        """,
        """
            @Composable
            fun <T> provided(value: T, %composer: Composer<*>?, %key: Int, %changed: Int): State<T> {
              %composer.startReplaceableGroup(%key)
              val tmp0 = state(null, {
                val tmp0_return = value
                tmp0_return
              }, %composer, <>, 0, 0b0001).apply {
                value = value
              }
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testReturnInlinedExpressionWithCall(): Unit = controlFlow(
        """
            import androidx.compose.state
            import androidx.compose.State

            @Composable
            fun Test(x: Int): Int {
                return x.let {
                    A()
                    123
                }
            }
        """,
        """
            @Composable
            fun Test(x: Int, %composer: Composer<*>?, %key: Int, %changed: Int): Int {
              %composer.startReplaceableGroup(%key)
              val tmp0 =
              val tmp1_group = x.let { it: Int ->
                A(%composer, <>, 0)
                val tmp0_return = 123
                tmp0_return
              }
              tmp1_group
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )
}