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

import android.widget.TextView
import androidx.compose.Composer
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.junit.Test
import org.junit.runner.RunWith
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
    fun testAccessibilityBridgeGeneration(): Unit = ensureSetup {
        compile(
            mapOf("library module" to mapOf(
                    "x/I.kt" to """
                      package x

                      import androidx.compose.Composable

                      @Composable fun bar(arg: @Composable () -> Unit) {
                          arg()
                      }
                  """.trimIndent()
                ),
                "Main" to mapOf(
                    "y/User.kt" to """
                      package y

                      import x.bar
                      import androidx.compose.Composable

                      @Composable fun baz() {
                          bar {
                            foo()
                          }
                      }
                      @Composable private fun foo() { }
                  """.trimIndent()
                )
            )
        ) {
            // Check that there is only one method declaration for access$foo.
            // We used to introduce more symbols for the same function leading
            // to multiple identical methods in the output.
            // In the dump, $ is mapped to %.
            val declaration = "synthetic access%foo"
            val occurrences = it.windowed(declaration.length) { candidate ->
                if (candidate.equals(declaration))
                    1
                else
                    0
            }.sum()
            assert(occurrences == 1)
        }
    }

    @Test
    fun testInlineClassCrossModule(): Unit = ensureSetup {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/I.kt" to """
                      package x
                      inline class I(val i: Int) {
                        val prop
                          get() = i + 1
                      }
                  """.trimIndent()
                ),
                "Main" to mapOf(
                    "y/User.kt" to """
                      package y
                      import x.I
                      inline class J(val j: Int)
                      fun foo(): Int = I(42).i + J(23).j + I(1).prop
                  """.trimIndent()
                )
            )
        ) {
            // If the output contains getI-impl, the cross-module inline class
            // was incorrectly compiled and the getter was not removed. This
            // happens if the relationship between the getter and the corresponding
            // property is broken by the compiler.
            assert(!it.contains("getI-impl"))
            // Check that inline classes where optimized to integers.
            assert(it.contains("INVOKESTATIC x/I.constructor-impl (I)I"))
            assert(it.contains("INVOKESTATIC y/J.constructor-impl (I)I"))
            // Check that the inline class prop getter is correctly mangled.
            assert(it.contains("INVOKESTATIC x/I.getProp-impl (I)I"))
        }
    }

    @Test
    fun testInlineClassOverloading(): Unit = ensureSetup {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/A.kt" to """
                        package x

                        import androidx.compose.Composable

                        inline class I(val i: Int)
                        inline class J(val j: Int)

                        @Composable fun foo(i: I) { }
                        @Composable fun foo(j: J) { }
                    """.trimIndent()
                ),
                "Main" to mapOf(
                    "y/B.kt" to """
                        package y

                        import androidx.compose.Composable
                        import x.*

                        @Composable fun bar(k: Int) {
                            foo(I(k))
                            foo(J(k))
                        }
                    """
                )
            )
        ) {
            // Check that the composable functions were properly mangled
            assert(it.contains("public final static foo-M7K8KNI(ILandroidx/compose/Composer;)V"))
            assert(it.contains("public final static foo-fpD6Y9w(ILandroidx/compose/Composer;)V"))
            // Check that we didn't leave any references to the original name, which probably
            // leads to a compile error.
            assert(!it.contains("foo("))
        }
    }

    @Test
    fun testCrossinlineEmittable(): Unit = ensureSetup {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/A.kt" to """
                    package x

                    import androidx.compose.Composable
                    import android.widget.LinearLayout

                    @Composable inline fun row(crossinline children: @Composable () -> Unit) {
                        LinearLayout {
                            children()
                        }
                    }
                 """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                    package b

                    import androidx.compose.Composable
                    import x.row

                    @Composable fun Test() {
                        row { }
                    }
                """
                )
            )
        )
    }

    @Test
    fun testConstCrossModule(): Unit = ensureSetup {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/A.kt" to """
                    package x

                    const val MyConstant: String = ""
                 """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                    package b

                    import x.MyConstant

                    fun Test(foo: String = MyConstant) {
                        print(foo)
                    }
                """
                )
            )
        ) {
            assert(it.contains("LDC \"\""))
            assert(!it.contains("INVOKESTATIC x/AKt.getMyConstant"))
        }
    }

    @Test
    fun testNonCrossinlineComposable(): Unit = ensureSetup {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/A.kt" to """
                    package x

                    import androidx.compose.Composable
                    import androidx.compose.Pivotal

                    @Composable
                    inline fun <T> key(
                        block: @Composable () -> T
                    ): T = block()
                 """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                    package b

                    import androidx.compose.Composable
                    import x.key

                    @Composable fun Test() {
                        key { }
                    }
                """
                )
            )
        )
    }

    @Test
    fun testNonCrossinlineComposableNoGenerics(): Unit = ensureSetup {
        compile(
            mapOf(
                "library module" to mapOf(
                    "x/A.kt" to """
                    package x

                    import androidx.compose.Composable
                    import androidx.compose.Pivotal

                    @Composable
                    inline fun key(
                        @Suppress("UNUSED_PARAMETER")
                        @Pivotal
                        v1: Int,
                        block: @Composable () -> Int
                    ): Int = block()
                 """
                ),
                "Main" to mapOf(
                    "b/B.kt" to """
                    package b

                    import androidx.compose.Composable
                    import x.key

                    @Composable fun Test() {
                        key(123) { 456 }
                    }
                """
                )
            )
        )
    }

    @Test
    fun testRemappedTypes(): Unit = ensureSetup {
        compile(
            mapOf(
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
    fun testInlineIssue(): Unit = ensureSetup {
        compile(
            mapOf(
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
    fun testInlineComposableProperty(): Unit = ensureSetup {
        compile(
            mapOf(
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
    fun testNestedInlineIssue(): Unit = ensureSetup {
        compile(
            mapOf(
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
    fun testComposerIntrinsicInline(): Unit = ensureSetup {
        compile(
            mapOf(
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
                    import androidx.compose.currentComposer

                    @Composable
                    inline fun abc(): Any {
                        return currentComposer
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
    fun testComposableOrderIssue(): Unit = ensureSetup {
        compile(
            mapOf(
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
    fun testSimpleXModuleCall(): Unit = ensureSetup {
        compile(
            mapOf(
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
    fun testJvmFieldIssue(): Unit = ensureSetup {
        compile(
            mapOf(
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
    fun testInstanceXModuleCall(): Unit = ensureSetup {
        compile(
            mapOf(
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
    fun testXModuleProperty(): Unit = ensureSetup {
        compile(
            mapOf(
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
    fun testXModuleInterface(): Unit = ensureSetup {
        compile(
            mapOf(
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
    fun testXModuleCtorComposableParam(): Unit = ensureSetup {
        compile(
            mapOf(
                "library module" to mapOf(
                    "a/Foo.kt" to """
                    package a

                    import androidx.compose.*

                    class Foo(val bar: @Composable () -> Unit)
                 """
                ),
                "Main" to mapOf(
                    "B.kt" to """
                    import a.Foo
                    import androidx.compose.*

                    @Composable fun Example(bar: @Composable () -> Unit) {
                        val foo = Foo(bar)
                    }
                """
                )
            )
        )
    }

    @Test
    fun testCrossModule_SimpleComposition(): Unit = ensureSetup {
        val tvId = 29

        compose(
            "TestF", mapOf(
                "library module" to mapOf(
                    "my/test/lib/InternalComp.kt" to """
                    package my.test.lib

                    import androidx.compose.*

                    @Composable fun InternalComp(block: @Composable () -> Unit) {
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
    fun testCrossModule_ComponentFunction(): Unit = ensureSetup {
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
    fun testCrossModule_ObjectFunction(): Unit = ensureSetup {
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
        modules: Map<String, Map<String, String>>,
        dumpClasses: Boolean = false,
        validate: ((String) -> Unit)? = null
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
        val outputFiles = (libraryClasses + appClasses).filter {
            it.relativePath.endsWith(".class")
        }

        if (validate != null) {
            validate(outputFiles.joinToString("\n") { it.asText().replace('$', '%') })
        }

        return outputFiles
    }

    fun compose(
        mainClassName: String,
        modules: Map<String, Map<String, String>>,
        dumpClasses: Boolean = false
    ): RobolectricComposeTester {
        val allClasses = compile(modules, dumpClasses)
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
        val composeMethod = instanceClass.getMethod("compose", Composer::class.java)

        return composeMulti({
            composeMethod.invoke(instanceOfClass, it)
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
