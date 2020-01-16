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
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.Compose
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment

const val ROOT_ID = 18284847

private class TestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply { id = ROOT_ID })
    }
}

private val Activity.root get() = findViewById(ROOT_ID) as ViewGroup

fun compose(composable: () -> Unit) =
    RobolectricComposeTester(composable)
fun composeMulti(composable: () -> Unit, advance: () -> Unit) =
    RobolectricComposeTester(composable, advance)

class RobolectricComposeTester internal constructor(
    val composable: () -> Unit,
    val advance: (() -> Unit)? = null
) {
    inner class ActiveTest(
        val activity: Activity,
        val advance: () -> Unit
    ) {
        fun then(block: (activity: Activity) -> Unit): ActiveTest {
            val scheduler = RuntimeEnvironment.getMasterScheduler()
            scheduler.advanceToLastPostedRunnable()
            advance()
            scheduler.advanceToLastPostedRunnable()
            block(activity)
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
        val composition = Compose.composeInto(root, null, composable)
        scheduler.advanceToLastPostedRunnable()
        block(activity)
        val advanceFn = advance ?: { composition.compose() }
        return ActiveTest(activity, advanceFn)
    }
}
