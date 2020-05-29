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

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(ComposeRobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class EmitTransformTests : AbstractIrTransformTest() {
    @Before
    fun before() {
        setUp()
    }
    private fun emitTransform(
        unchecked: String,
        checked: String,
        expectedTransformed: String,
        dumpTree: Boolean = false
    ) = verifyComposeIrTransform(
        """
            import androidx.compose.Composable
            import androidx.compose.Direct
            import android.widget.TextView
            import android.widget.LinearLayout

            $checked
        """.trimIndent(),
        expectedTransformed,
        """
            import androidx.compose.Composable
            import androidx.compose.Direct
            import android.widget.TextView
            import android.widget.LinearLayout

            $unchecked
        """.trimIndent(),
        dumpTree
    )

    @Test
    fun testSimpleEmit2(): Unit = emitTransform(
        """
        """,
        """
            import androidx.compose.state
            import androidx.compose.remember
            import android.widget.Button

            @Composable
            fun App() {
                val cond = state { true }
                val text = if (cond.value) remember { "abc" } else remember { "def" }
                Button(id=1, text=text, onClickListener={ cond.value = !cond.value })
            }
        """,
        """
            @Composable
            fun App(%composer: Composer<*>?, %key: Int, %changed: Int) {
              %composer.startRestartGroup(%key)
              if (%changed !== 0 || !%composer.skipping) {
                val cond = state(null, {
                  val tmp0_return = true
                  tmp0_return
                }, %composer, <>, 0, 0b0001)
                val text = if (cond.value) {
                  %composer.startReplaceableGroup(<>)
                  val tmp0_group = remember({
                    val tmp0_return = "abc"
                    tmp0_return
                  }, %composer, <>, 0)
                  %composer.endReplaceableGroup()
                  tmp0_group
                } else {
                  %composer.startReplaceableGroup(<>)
                  val tmp1_group = remember({
                    val tmp0_return = "def"
                    tmp0_return
                  }, %composer, <>, 0)
                  %composer.endReplaceableGroup()
                  tmp1_group
                }
                val tmp0 = text
                val tmp1 = remember(cond, {
                  { it: View? ->
                    cond.value = !cond.value
                  }
                }, %composer, <>, 0)
                %composer.emit(1124847890, { context: @[ParameterName(name = 'context')] Context ->
                  Button(context)
                }
                ) {
                  set(1) { p0: Int ->
                    setId(p0)
                  }
                  set(tmp0) { p0: CharSequence? ->
                    setText(p0)
                  }
                  set(tmp1) { p0: Function1<View?, Unit>? ->
                    setOnClickListener(p0)
                  }
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %key: Int, %force: Int ->
                App(%composer, %key, %changed or 0b0001)
              }
            }
        """
    )
}