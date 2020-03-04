/*
 * Copyright 2019 The Android Open Source Project
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

class ComposeCallResolutionDiagnosticsTests : AbstractComposeDiagnosticsTest() {

    private var isSetup = false
    override fun setUp() {
        isSetup = true
        super.setUp()
    }

    private fun <T> ensureSetup(block: () -> T): T {
        if (!isSetup) setUp()
        return block()
    }

    private fun setupAndDoTest(text: String) = ensureSetup { doTest(text) }

    fun testImplicitlyPassedReceiverScope1() = setupAndDoTest(
        """
            import androidx.compose.*
            import android.widget.*
            import android.os.Bundle
            import android.app.Activity
            import android.widget.FrameLayout

            val x: Any? = null

            fun Activity.setViewContent(composable: @Composable() () -> Unit): Composition? {
                assert(composable != x)
                return null
            }

            open class WebComponentActivity : Activity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)

                    setViewContent {
                        FrameLayout {
                        }
                    }
                }
            }
        """
    )

    fun testSimpleReceiverScope() = setupAndDoTest(
        """
            import android.widget.FrameLayout
            import androidx.compose.Composable
            import androidx.ui.node.UiComposer

            class SomeScope {
             val composer: UiComposer get() = error("should not be called")
            }

            @Composable fun SomeScope.foo() {
                FrameLayout { }
            }

        """
    )
}
