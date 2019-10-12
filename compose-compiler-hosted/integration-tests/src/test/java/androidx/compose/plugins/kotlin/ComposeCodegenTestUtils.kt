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

@file:Suppress("MemberVisibilityCanBePrivate")

package androidx.compose.plugins.kotlin

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.Component
import androidx.compose.CompositionContext
import androidx.compose.Compose
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.URLClassLoader

var uniqueNumber = 0

fun loadClass(loader: ClassLoader, name: String?, bytes: ByteArray): Class<*> {
    val defineClassMethod = ClassLoader::class.javaObjectType.getDeclaredMethod(
        "defineClass",
        String::class.javaObjectType,
        ByteArray::class.javaObjectType,
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType
    )
    defineClassMethod.isAccessible = true
    return defineClassMethod.invoke(loader, name, bytes, 0, bytes.size) as Class<*>
}

const val ROOT_ID = 18284847

private class TestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply { id = ROOT_ID })
    }
}

private val Activity.root get() = findViewById(ROOT_ID) as ViewGroup

private class Root(val composable: () -> Unit) : Component() {
    override fun compose() = composable()
}

class CompositionTest(val composable: () -> Unit) {

    inner class ActiveTest(val activity: Activity, val cc: CompositionContext) {
        fun then(block: (activity: Activity) -> Unit): ActiveTest {
            val scheduler = RuntimeEnvironment.getMasterScheduler()
            scheduler.advanceToLastPostedRunnable()
            cc.compose()
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
        val component = Root(composable)
        val cc = Compose.createCompositionContext(root.context, root, component, null)
        return ActiveTest(activity, cc).then(block)
    }
}

fun compose(composable: () -> Unit) = CompositionTest(composable)