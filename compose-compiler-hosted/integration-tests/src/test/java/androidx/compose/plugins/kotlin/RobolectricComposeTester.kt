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

import android.app.Activity
import android.os.Bundle
import android.os.Looper.getMainLooper
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.Composer
import androidx.compose.Composition
import androidx.compose.ExperimentalComposeApi
import androidx.compose.Recomposer
import androidx.compose.compositionFor
import androidx.ui.core.ContextAmbient
import androidx.ui.node.UiApplier
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

const val ROOT_ID = 18284847

private class TestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply { id = ROOT_ID })
    }
}

private val Activity.root get() = findViewById(ROOT_ID) as ViewGroup

fun compose(composable: (Composer<*>, Int, Int) -> Unit) =
    RobolectricComposeTester(composable)
fun composeMulti(composable: (Composer<*>, Int, Int) -> Unit, advance: () -> Unit) =
    RobolectricComposeTester(composable, advance)

class RobolectricComposeTester internal constructor(
    val composable: (Composer<*>, Int, Int) -> Unit,
    val advance: (() -> Unit)? = null
) {
    inner class ActiveTest(
        val activity: Activity,
        val advance: () -> Unit
    ) {
        fun then(block: (activity: Activity) -> Unit): ActiveTest {
            try {
                val scheduler = RuntimeEnvironment.getMasterScheduler()
                scheduler.advanceToLastPostedRunnable()
                advance()
                scheduler.advanceToLastPostedRunnable()
                block(activity)
            } catch (e: Throwable) {
                shadowOf(getMainLooper()).idle()
                throw e
            }
            return this
        }
    }

    fun then(block: (activity: Activity) -> Unit): ActiveTest {
        val scheduler = RuntimeEnvironment.getMasterScheduler()
        scheduler.pause()
        val controller = Robolectric.buildActivity(TestActivity::class.java)
        val activity = controller.create().get()
        val root = activity.root
        scheduler.advanceToLastPostedRunnable()

        val startProviders = Composer::class.java.methods.first {
            it.name.startsWith("startProviders")
        }
        val endProviders = Composer::class.java.methods.first {
            it.name.startsWith("endProviders")
        }
        val setContentMethod = Composition::class.java.methods.first { it.name == "setContent" }
        startProviders.isAccessible = true
        endProviders.isAccessible = true
        setContentMethod.isAccessible = true

        val realComposable: (Composer<*>, Int, Int) -> Unit = { composer, _, _ ->
            startProviders.invoke(
                composer,
                listOf(ContextAmbient provides root.context).toTypedArray()
            )
            composable(composer, 0, 0)
            endProviders.invoke(composer)
        }

        @OptIn(ExperimentalComposeApi::class)
        val composition = compositionFor(root, UiApplier(root), Recomposer.current())
        fun setContent() {
            setContentMethod.invoke(composition, realComposable)
        }
        setContent()
        scheduler.advanceToLastPostedRunnable()
        block(activity)
        val advanceFn = advance ?: { setContent() }
        return ActiveTest(activity, advanceFn)
    }
}
