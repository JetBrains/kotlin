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
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.Compose
import androidx.compose.Composer
import androidx.compose.currentComposerNonNull
import androidx.compose.runWithCurrent
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.net.URLClassLoader

@RunWith(ComposeRobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class KtxCrossModuleTests : AbstractCodegenTest() {

    @Test
    fun testRemappedTypes(): Unit = forComposerParam(true, false) {
        compile(
            "TestG", mapOf(
                "library module" to mapOf(
                    "x/A.kt" to """
                    package x

                    class A {
                        fun makeA(): A { return A() }
                        fun makeB(): B { return B() }
                        class B() {
                        }
                    }
                 """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                    package b

                    import x.A

                    class C {
                        fun useAB() {
                            val a = A()
                            a.makeA()
                            a.makeB()
                            val b = A.B()
                        }
                    }
                """
                )
            )
        )
    }

    @Test
    fun testInlineIssue(): Unit = forComposerParam(true, false) {
        compile(
            "TestG", mapOf(
                "library module" to mapOf(
                    "x/C.kt" to """
                    fun ghi() {
                        abc {}
                    }
                    """,
                    "x/A.kt" to """
                    inline fun abc(fn: () -> Unit) {
                        fn()
                    }
                    """,
                    "x/B.kt" to """
                    fun def() {
                        abc {}
                    }
                    """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                """
                )
            )
        )
    }

    @Test
    fun testInlineComposableProperty(): Unit = forComposerParam(true, false) {
        compile(
            "TestG", mapOf(
                "library module" to mapOf(
                    "x/A.kt" to """
                    package x

                    import androidx.compose.Composable

                    class Foo {
                      @Composable val value: Int get() = 123
                    }
                 """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                    package b

                    import androidx.compose.Composable
                    import x.Foo

                    val foo = Foo()

                    @Composable fun Test() {
                        print(foo.value)
                    }
                """
                )
            )
        )
    }

    @Test
    fun testNestedInlineIssue(): Unit = forComposerParam(true, false) {
        compile(
            "TestG", mapOf(
                "library module" to mapOf(
                    "x/C.kt" to """
                    fun ghi() {
                        abc {
                            abc {

                            }
                        }
                    }
                    """,
                    "x/A.kt" to """
                    inline fun abc(fn: () -> Unit) {
                        fn()
                    }
                    """,
                    "x/B.kt" to """
                    fun def() {
                        abc {
                            abc {

                            }
                        }
                    }
                    """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                """
                )
            )
        )
    }

    @Test
    fun testComposerIntrinsicInline(): Unit = forComposerParam(true, false) {
        compile(
            "TestG", mapOf(
                "library module" to mapOf(
                    "x/C.kt" to """
                    import androidx.compose.Composable

                    @Composable
                    fun ghi() {
                        val x = abc()
                        print(x)
                    }
                    """,
                    "x/A.kt" to """
                    import androidx.compose.Composable
                    import androidx.compose.currentComposerIntrinsic

                    @Composable
                    inline fun abc(): Any {
                        return currentComposerIntrinsic
                    }
                    """,
                    "x/B.kt" to """
                    import androidx.compose.Composable

                    @Composable
                    fun def() {
                        val x = abc()
                        print(x)
                    }
                    """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                """
                )
            )
        )
    }


    @Test
    fun testComposableOrderIssue(): Unit = forComposerParam(true, false) {
        compile(
            "TestG", mapOf(
                "library module" to mapOf(
                    "C.kt" to """
                    import androidx.compose.*

                    @Composable
                    fun b() {
                        a()
                    }
                    """,
                    "A.kt" to """
                    import androidx.compose.*

                    @Composable
                    fun a() {

                    }
                    """,
                    "B.kt" to """
                    import androidx.compose.*

                    @Composable
                    fun c() {
                        a()
                    }

                    """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                """
                )
            )
        )
    }

    @Test
    fun testSimpleXModuleCall(): Unit = forComposerParam(true, false) {
        compile(
            "TestG", mapOf(
                "library module" to mapOf(
                    "a/A.kt" to """
                    package a

                    import androidx.compose.*

                    @Composable
                    fun FromA() {}
                 """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                    package b

                    import a.FromA
                    import androidx.compose.*

                    @Composable
                    fun FromB() {
                        FromA()
                    }
                """
                )
            )
        )
    }

    @Test
    fun testJvmFieldIssue(): Unit = forComposerParam(true, false) {
        compile(
            "TestG", mapOf(
                "library module" to mapOf(
                    "x/C.kt" to """
                    fun Test2() {
                      bar = 10
                      print(bar)
                    }
                    """,
                    "x/A.kt" to """
                      @JvmField var bar: Int = 0
                    """,
                    "x/B.kt" to """
                    fun Test() {
                      bar = 10
                      print(bar)
                    }
                    """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                """
                )
            )
        )
    }

    @Test
    fun testInstanceXModuleCall(): Unit = forComposerParam(true, false) {
        compile(
            "TestH", mapOf(
                "library module" to mapOf(
                    "a/Foo.kt" to """
                    package a

                    import androidx.compose.*

                    class Foo {
                        @Composable
                        fun FromA() {}
                    }
                 """
                ),
                "Main" to mapOf(
                    "B.kt" to """
                    import a.Foo
                    import androidx.compose.*

                    @Composable
                    fun FromB() {
                        Foo().FromA()
                    }
                """
                )
            )
        )
    }

    @Test
    fun testXModuleProperty(): Unit = forComposerParam(true, false) {
        compile(
            "TestI", mapOf(
                "library module" to mapOf(
                    "a/Foo.kt" to """
                    package a

                    import androidx.compose.*

                    @Composable val foo: Int get() { return 123 }
                 """
                ),
                "Main" to mapOf(
                    "B.kt" to """
                    import a.foo
                    import androidx.compose.*

                    @Composable
                    fun FromB() {
                        foo
                    }
                """
                )
            )
        )
    }

    @Test
    fun testXModuleInterface(): Unit = forComposerParam(true, false) {
        compile(
            "TestJ", mapOf(
                "library module" to mapOf(
                    "a/Foo.kt" to """
                    package a

                    import androidx.compose.*

                    interface Foo {
                        @Composable fun foo()
                    }
                 """
                ),
                "Main" to mapOf(
                    "B.kt" to """
                    import a.Foo
                    import androidx.compose.*

                    class B : Foo {
                        @Composable override fun foo() {}
                    }

                    @Composable fun Example(inst: Foo) {
                        B().foo()
                        inst.foo()
                    }
                """
                )
            )
        )
    }

    @Test
    fun testCrossModule_SimpleComposition(): Unit = forComposerParam(/*true, */false) {
        val tvId = 29

        compose(
            "TestF", mapOf(
                "library module" to mapOf(
                    "my/test/lib/InternalComp.kt" to """
                    package my.test.lib

                    import androidx.compose.*

                    @Composable fun InternalComp(block: @Composable() () -> Unit) {
                        block()
                    }
                 """
                ),
                "Main" to mapOf(
                    "my/test/app/Main.kt" to """
                   package my.test.app

                   import android.widget.*
                   import androidx.compose.*
                   import my.test.lib.*

                   var bar = 0
                   var doRecompose: () -> Unit = {}

                   class TestF {
                       @Composable
                       fun compose() {
                         Recompose { recompose ->
                           doRecompose = recompose
                           Foo(bar)
                         }
                       }

                       fun advance() {
                         bar++
                         doRecompose()
                       }
                   }

                   @Composable
                   fun Foo(bar: Int) {
                     InternalComp {
                       TextView(text="${'$'}bar", id=$tvId)
                     }
                   }
                """
                )
            )
        ).then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("0", tv.text)
        }.then { activity ->
            val tv = activity.findViewById(tvId) as TextView
            assertEquals("1", tv.text)
        }
    }

    @Test
    fun testCrossModule_ComponentFunction(): Unit = forComposerParam(/*true, */false) {
        val tvName = 101
        val tvAge = 102

        compose(
            "TestF", mapOf(
                "library KTX module" to mapOf(
                    "my/test2/lib/ktx/ComponentFunction.kt" to """
                       package my.test2.ktx

                       import android.widget.*
                       import androidx.compose.*

                       @Composable
                       fun ComponentFunction(name: String, age: Int) {
                         LinearLayout {
                           TextView(text=name, id=$tvName)
                           TextView(text="${'$'}age", id=$tvAge)
                         }
                       }
                 """
                ),
                "Main" to mapOf(
                    "my/test2/app/Test.kt" to """
                       package my.test2.app

                       import android.widget.*
                       import androidx.compose.*
                       import my.test2.ktx.*

                       var age = $PRESIDENT_AGE_1
                       var name = "$PRESIDENT_NAME_1"
                       var doRecompose: () -> Unit = {}

                       class TestF {
                           @Composable
                           fun compose() {
                             Recompose { recompose ->
                               doRecompose = recompose
                               Foo(name=name, age=age)
                             }
                           }

                           fun advance() {
                             age = $PRESIDENT_AGE_16
                             name = "$PRESIDENT_NAME_16"
                             doRecompose()
                           }
                       }

                       @Composable
                       fun Foo(name: String, age: Int) {
                         ComponentFunction(name, age)
                       }
                    """
                )
            )
        ).then { activity ->
            val name = activity.findViewById(tvName) as TextView
            assertEquals(PRESIDENT_NAME_1, name.text)
            val age = activity.findViewById(tvAge) as TextView
            assertEquals("$PRESIDENT_AGE_1", age.text)
        }.then { activity ->
            val name = activity.findViewById(tvName) as TextView
            assertEquals(PRESIDENT_NAME_16, name.text)
            val age = activity.findViewById(tvAge) as TextView
            assertEquals("$PRESIDENT_AGE_16", age.text)
        }
    }

    @Test
    fun testCrossModule_ObjectFunction(): Unit = forComposerParam(/*true, */false) {
        val tvName = 101
        val tvAge = 102

        compose(
            "TestF", mapOf(
                "library KTX module" to mapOf(
                    "my/test2/lib/ktx/ObjectFunction.kt" to """
                       package my.test2.ktx

                       import android.widget.*
                       import androidx.compose.*

                       object Container {
                           @Composable
                           fun ComponentFunction(name: String, age: Int) {
                             LinearLayout {
                               TextView(text=name, id=$tvName)
                               TextView(text="${'$'}age", id=$tvAge)
                             }
                           }
                       }
                 """
                ),
                "Main" to mapOf(
                    "my/test2/app/Test.kt" to """
                       package my.test2.app

                       import android.widget.*
                       import androidx.compose.*
                       import my.test2.ktx.*

                       var age = $PRESIDENT_AGE_1
                       var name = "$PRESIDENT_NAME_1"
                       var doRecompose: () -> Unit = {}

                       class TestF {
                           @Composable
                           fun compose() {
                             Recompose { recompose ->
                               doRecompose = recompose
                               Foo(name, age)
                             }
                           }

                           fun advance() {
                             age = $PRESIDENT_AGE_16
                             name = "$PRESIDENT_NAME_16"
                             doRecompose()
                           }
                       }

                       @Composable
                       fun Foo(name: String, age: Int) {
                         Container.ComponentFunction(name, age)
                       }
                    """
                )
            )
        ).then { activity ->
            val name = activity.findViewById(tvName) as TextView
            assertEquals(PRESIDENT_NAME_1, name.text)
            val age = activity.findViewById(tvAge) as TextView
            assertEquals("$PRESIDENT_AGE_1", age.text)
        }.then { activity ->
            val name = activity.findViewById(tvName) as TextView
            assertEquals(PRESIDENT_NAME_16, name.text)
            val age = activity.findViewById(tvAge) as TextView
            assertEquals("$PRESIDENT_AGE_16", age.text)
        }
    }

    fun compile(
        mainClassName: String,
        modules: Map<String, Map<String, String>>,
        dumpClasses: Boolean = false
    ): List<OutputFile> {
        val libraryClasses = (modules.filter { it.key != "Main" }.map {
            // Setup for compile
            this.classFileFactory = null
            this.myEnvironment = null
            setUp(it.key.contains("--ktx=false"))

            classLoader(it.value, dumpClasses).allGeneratedFiles.also {
                // Write the files to the class directory so they can be used by the next module
                // and the application
                it.writeToDir(classesDirectory)
            }
        } + emptyList()).reduce { acc, mutableList -> acc + mutableList }

        // Setup for compile
        this.classFileFactory = null
        this.myEnvironment = null
        setUp()

        // compile the next one
        val appClasses = classLoader(modules["Main"]
            ?: error("No Main module specified"), dumpClasses).allGeneratedFiles

        // Load the files looking for mainClassName
        return (libraryClasses + appClasses).filter {
            it.relativePath.endsWith(".class")
        }
    }

    fun compose(
        mainClassName: String,
        modules: Map<String, Map<String, String>>,
        dumpClasses: Boolean = false
    ): RobolectricComposeTester {
        val allClasses = compile(mainClassName, modules, dumpClasses)
        val loader = URLClassLoader(emptyArray(), this.javaClass.classLoader)
        val instanceClass = run {
            var instanceClass: Class<*>? = null
            var loadedOne = false
            for (outFile in allClasses) {
                val bytes = outFile.asByteArray()
                val loadedClass = loadClass(loader, null, bytes)
                if (loadedClass.name.endsWith(mainClassName)) instanceClass = loadedClass
                loadedOne = true
            }
            if (!loadedOne) error("No classes loaded")
            instanceClass ?: error("Could not find class $mainClassName in loaded classes")
        }

        val instanceOfClass = instanceClass.newInstance()
        val advanceMethod = instanceClass.getMethod("advance")
        val composeMethod = if (ComposeFlags.COMPOSER_PARAM)
            instanceClass.getMethod("compose", Composer::class.java)
        else
            instanceClass.getMethod("compose")

        return composeMulti({
            if (ComposeFlags.COMPOSER_PARAM)
                composeMethod.invoke(instanceOfClass, currentComposerNonNull)
            else
                composeMethod.invoke(instanceOfClass)
        }) {
            advanceMethod.invoke(instanceOfClass)
        }
    }

    fun setUp(disable: Boolean = false) {
        if (disable) {
            this.disableIrAndKtx = true
            try {
                setUp()
            } finally {
                this.disableIrAndKtx = false
            }
        } else {
            setUp()
        }
    }

    override fun setUp() {
        if (disableIrAndKtx) {
            super.setUp()
        } else {
            super.setUp()
        }
    }

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        if (!disableIrAndKtx) {
            super.setupEnvironment(environment)
        }
    }

    private var disableIrAndKtx = false

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        super.updateConfiguration(configuration)
        if (disableIrAndKtx) {
            configuration.put(JVMConfigurationKeys.IR, false)
        }
    }

    private var testLocalUnique = 0
    private var classesDirectory = tmpDir(
        "kotlin-${testLocalUnique++}-classes"
    )

    override val additionalPaths: List<File> = listOf(classesDirectory)
}

fun OutputFile.writeToDir(directory: File) =
    FileUtil.writeToFile(File(directory, relativePath), asByteArray())

fun Collection<OutputFile>.writeToDir(directory: File) = forEach { it.writeToDir(directory) }

private fun tmpDir(name: String): File {
    return FileUtil.createTempDirectory(name, "", false).canonicalFile
}
