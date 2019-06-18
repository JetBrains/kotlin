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

import android.app.Activity
import android.view.ViewGroup
import android.widget.TextView
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import androidx.compose.FrameManager

@RunWith(ComposeRobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class FcsModelCodeGenTests : AbstractCodegenTest() {

    @Before
    fun before() {
        val scheduler = RuntimeEnvironment.getMasterScheduler()
        scheduler.pause()
    }

    override fun setUp() {
        isSetup = true
        super.setUp()
    }

    private var isSetup = false
    private inline fun <T> ensureSetup(crossinline block: () -> T): T {
        if (!isSetup) setUp()
        return FrameManager.isolated { block() }
    }

    @Test
    fun testCGModelView_PersonModel(): Unit = ensureSetup {
        val tvNameId = 384
        val tvAgeId = 385

        var name = PRESIDENT_NAME_1
        var age = PRESIDENT_AGE_1
        compose(
            """
            @Model
            class FcsPerson4(var name: String, var age: Int)

            @Composable
            fun PersonView4(person: FcsPerson4) {
              Observe {
                TextView(text=person.name, id=$tvNameId)
                TextView(text=person.age.toString(), id=$tvAgeId)
              }
            }

            val president = FcsPerson4("$PRESIDENT_NAME_1", $PRESIDENT_AGE_1)
            """, { mapOf("name" to name, "age" to age) }, """
               president.name = name
               president.age = age
            """, """
                PersonView4(person=president)
            """).then { activity ->
            val tvName = activity.findViewById(tvNameId) as TextView
            val tvAge = activity.findViewById(tvAgeId) as TextView
            assertEquals(PRESIDENT_NAME_1, tvName.text)
            assertEquals(PRESIDENT_AGE_1.toString(), tvAge.text)

            name = PRESIDENT_NAME_16
            age = PRESIDENT_AGE_16
        }.then { activity ->
            val tvName = activity.findViewById(tvNameId) as TextView
            val tvAge = activity.findViewById(tvAgeId) as TextView
            assertEquals(PRESIDENT_NAME_16, tvName.text)
            assertEquals(PRESIDENT_AGE_16.toString(), tvAge.text)
        }
    }

    @Test // b/120843442
    fun testCGModelView_ObjectModel(): Unit = ensureSetup {
        val tvNameId = 384
        val tvAgeId = 385

        var name = PRESIDENT_NAME_1
        var age = PRESIDENT_AGE_1
        compose(
            """
            @Model
            object fcs_president {
                var name: String = "$PRESIDENT_NAME_1"
                var age: Int = $PRESIDENT_AGE_1
            }

            @Composable
            fun PresidentView() {
              Observe {
                TextView(text=fcs_president.name, id=$tvNameId)
                TextView(text=fcs_president.age.toString(), id=$tvAgeId)
              }
            }


            """, { mapOf("name" to name, "age" to age) }, """
               fcs_president.name = name
               fcs_president.age = age
            """, """
                PresidentView()
            """).then { activity ->
            val tvName = activity.findViewById(tvNameId) as TextView
            val tvAge = activity.findViewById(tvAgeId) as TextView
            assertEquals(PRESIDENT_NAME_1, tvName.text)
            assertEquals(PRESIDENT_AGE_1.toString(), tvAge.text)

            name = PRESIDENT_NAME_16
            age = PRESIDENT_AGE_16
        }.then { activity ->
            val tvName = activity.findViewById(tvNameId) as TextView
            val tvAge = activity.findViewById(tvAgeId) as TextView
            assertEquals(PRESIDENT_NAME_16, tvName.text)
            assertEquals(PRESIDENT_AGE_16.toString(), tvAge.text)
        }
    }

    @Test // b/120836313
    fun testCGModelView_DataModel(): Unit = ensureSetup {
        val tvNameId = 384
        val tvAgeId = 385

        var name = PRESIDENT_NAME_1
        var age = PRESIDENT_AGE_1
        compose(
            """
            @Model
            data class FcsPersonB(var name: String, var age: Int)

            @Composable
            fun PersonViewB(person: FcsPersonB) {
              Observe {
                TextView(text=person.name, id=$tvNameId)
                TextView(text=person.age.toString(), id=$tvAgeId)
              }
            }

            val president = FcsPersonB("$PRESIDENT_NAME_1", $PRESIDENT_AGE_1)
            """, { mapOf("name" to name, "age" to age) }, """
               president.name = name
               president.age = age
            """, """
                PersonViewB(person=president)
            """).then { activity ->
            val tvName = activity.findViewById(tvNameId) as TextView
            val tvAge = activity.findViewById(tvAgeId) as TextView
            assertEquals(PRESIDENT_NAME_1, tvName.text)
            assertEquals(PRESIDENT_AGE_1.toString(), tvAge.text)

            name = PRESIDENT_NAME_16
            age = PRESIDENT_AGE_16
        }.then { activity ->
            val tvName = activity.findViewById(tvNameId) as TextView
            val tvAge = activity.findViewById(tvAgeId) as TextView
            assertEquals(PRESIDENT_NAME_16, tvName.text)
            assertEquals(PRESIDENT_AGE_16.toString(), tvAge.text)
        }
    }

    @Test // b/120843442
    fun testCGModelView_ZeroFrame(): Unit = ensureSetup {
        val tvNameId = 384
        val tvAgeId = 385

        var name = PRESIDENT_NAME_1
        var age = PRESIDENT_AGE_1
        compose(
            """
            @Model
            class FcsPersonC(var name: String, var age: Int)

            @Composable
            fun PersonViewC(person: FcsPersonC) {
              Observe {
                TextView(text=person.name, id=$tvNameId)
                TextView(text=person.age.toString(), id=$tvAgeId)
              }
            }

            val president = FrameManager.unframed { FcsPersonC("$PRESIDENT_NAME_1", $PRESIDENT_AGE_1) }
            """, { mapOf("name" to name, "age" to age) }, """
               president.name = name
               president.age = age
            """, """
                PersonViewC(person=president)
            """).then { activity ->
            val tvName = activity.findViewById(tvNameId) as TextView
            val tvAge = activity.findViewById(tvAgeId) as TextView
            assertEquals(name, tvName.text)
            assertEquals(age.toString(), tvAge.text)

            name = PRESIDENT_NAME_16
            age = PRESIDENT_AGE_16
        }.then { activity ->
            val tvName = activity.findViewById(tvNameId) as TextView
            val tvAge = activity.findViewById(tvAgeId) as TextView
            assertEquals(PRESIDENT_NAME_16, tvName.text)
            assertEquals(PRESIDENT_AGE_16.toString(), tvAge.text)
        }
    }

    @Test // b/120843442
    fun testCGModelView_ZeroFrame_Modification(): Unit = ensureSetup {
        val tvNameId = 384
        val tvAgeId = 385

        var name = PRESIDENT_NAME_1
        var age = PRESIDENT_AGE_1
        compose(
            """
            @Model
            class FcsPersonD(var name: String, var age: Int)

            @Composable
            fun PersonViewD(person: FcsPersonD) {
              Observe {
                TextView(text=person.name, id=$tvNameId)
                TextView(text=person.age.toString(), id=$tvAgeId)
              }
            }

            val president = FrameManager.framed { FcsPersonD("$PRESIDENT_NAME_1", $PRESIDENT_AGE_1).apply { age = $PRESIDENT_AGE_1 } }
            """, { mapOf("name" to name, "age" to age) }, """
               president.name = name
               president.age = age
            """, """
                PersonViewD(person=president)
            """).then { activity ->
            val tvName = activity.findViewById(tvNameId) as TextView
            val tvAge = activity.findViewById(tvAgeId) as TextView
            assertEquals(PRESIDENT_NAME_1, tvName.text)
            assertEquals(PRESIDENT_AGE_1.toString(), tvAge.text)

            name = PRESIDENT_NAME_16
            age = PRESIDENT_AGE_16
        }.then { activity ->
            val tvName = activity.findViewById(tvNameId) as TextView
            val tvAge = activity.findViewById(tvAgeId) as TextView
            assertEquals(PRESIDENT_NAME_16, tvName.text)
            assertEquals(PRESIDENT_AGE_16.toString(), tvAge.text)
        }
    }

    fun compose(
        prefix: String,
        valuesFactory: () -> Map<String, Any>,
        advance: String,
        composition: String,
        dumpClasses: Boolean = false
    ): ModelCompositionTest {
        val className = "Test_${uniqueNumber++}"
        val fileName = "$className.kt"

        val candidateValues = valuesFactory()

        @Suppress("NO_REFLECTION_IN_CLASS_PATH")
        val parameterList = candidateValues.map {
            "${it.key}: ${it.value::class.qualifiedName}"
        }.joinToString()
        val parameterTypes = candidateValues.map {
            it.value::class.javaPrimitiveType ?: it.value::class.javaObjectType
        }.toTypedArray()

        val compiledClasses = classLoader("""
           import android.content.Context
           import android.widget.*
           import androidx.compose.*

           $prefix

           class $className {

             fun compose() {
               $composition
             }

             fun advance($parameterList) {
               $advance
             }
           }
        """, fileName, dumpClasses)

        val allClassFiles = compiledClasses.allGeneratedFiles.filter {
            it.relativePath.endsWith(".class")
        }

        val instanceClass = run {
            var instanceClass: Class<*>? = null
            var loadedOne = false
            for (outFile in allClassFiles) {
                val bytes = outFile.asByteArray()
                val loadedClass = loadClass(
                    this.javaClass.classLoader!!,
                    null,
                    bytes
                )
                if (loadedClass.name == className) instanceClass = loadedClass
                loadedOne = true
            }
            if (!loadedOne) error("No classes loaded")
            instanceClass ?: error("Could not find class $className in loaded classes")
        }

        val instanceOfClass = instanceClass.newInstance()
        val advanceMethod = instanceClass.getMethod("advance", *parameterTypes)
        val composeMethod = instanceClass.getMethod("compose")

        return composeModel({ composeMethod.invoke(instanceOfClass) }) {
            val values = valuesFactory()
            val arguments = values.map { it.value }.toTypedArray()
            advanceMethod.invoke(instanceOfClass, *arguments)
        }
    }

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        environment.configuration.put(
            ComposeConfigurationKeys.COMPOSABLE_CHECKER_MODE_KEY,
            ComposableAnnotationChecker.Mode.FCS
        )
        super.setupEnvironment(environment)
    }
}

private val Activity.root get() = findViewById(ROOT_ID) as ViewGroup

private fun composeModel(composable: () -> Unit, advance: () -> Unit) =
    ModelCompositionTest(composable, advance)