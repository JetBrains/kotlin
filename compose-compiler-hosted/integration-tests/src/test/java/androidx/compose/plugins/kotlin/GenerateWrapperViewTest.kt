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

import junit.framework.TestCase

class GenerateWrapperViewTest : AbstractCodegenTest() {

    fun testPlaceholder() {
        // do nothing, in order to prevent warning
    }

    fun xtestWrapperViewGeneration() {

        val klass = loadClass("MainComponent", """
            import android.app.Activity
            import android.os.Bundle
            import androidx.compose.Component
            import androidx.compose.CompositionContext

            class MainActivity : Activity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    val inst = MainComponent.createInstance(this)
                    inst.setFoo("string")
                    setContentView(inst)
                }
            }

            class MainComponent : Component() {
                lateinit var foo: String
                override fun compose() {}
            }
        """)

        val wrapperClass = klass.declaredClasses.find {
            it.name == "MainComponent\$MainComponentWrapperView"
        }
        TestCase.assertNotNull("wrapper view gets generated", wrapperClass)
        if (wrapperClass == null) return
        TestCase.assertEquals(
            "Wrapper view subclasses LinearLayout", "android.widget.LinearLayout",
            wrapperClass.superclass?.name
        )
        val setFoo = wrapperClass.declaredMethods.find { it.name == "setFoo" }
        TestCase.assertNotNull("has a setter method for properties", setFoo)

        val companionClass =
            klass.declaredClasses.find { it.name == "MainComponent\$R4HStaticRenderCompanion" }
        TestCase.assertNotNull("companion class gets generated", companionClass)
        if (companionClass == null) return
        val createInstanceFn = companionClass.declaredMethods.find { it.name == "createInstance" }
        TestCase.assertNotNull("createInstance function gets generated", createInstanceFn)
    }
}