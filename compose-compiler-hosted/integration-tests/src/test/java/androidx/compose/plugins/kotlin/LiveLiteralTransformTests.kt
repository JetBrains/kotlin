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

import androidx.compose.plugins.kotlin.compiler.lower.DurableKeyVisitor
import androidx.compose.plugins.kotlin.compiler.lower.LiveLiteralTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.IrProvider
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.junit.Test

class LiveLiteralTransformTests : AbstractIrTransformTest() {

    fun testSiblingCallArgs() = assertNoDuplicateKeys(
        """
        fun Test() {
            print(1)
            print(1)
        }
        """
    )

    fun testFunctionCallWithConstArg() = assertKeys(
        "Int%arg-0%call-print%fun-Test",
        "Int%arg-0%call-print-1%fun-Test"
    ) {
        """
        fun Test() {
            print(1)
            print(1)
        }
        """
    }

    fun testDispatchReceiver() = assertKeys(
        "Int%%this%call-toString%arg-0%call-print%fun-Test",
        "Int%arg-0%call-print-1%fun-Test"
    ) {
        """
        fun Test() {
            print(1.toString())
            print(1)
        }
        """
    }

    fun testInsidePropertyGetter() = assertKeys(
        "Int%fun-%get-foo%%get%val-foo"
    ) {
        """
        val foo: Int get() = 1
        """
    }

    // NOTE(lmr): For static initializer expressions we can/should do more.
    fun testInsidePropertyInitializer() = assertKeys {
        """
        val foo: Int = 1
        """
    }

    fun testValueParameter() = assertKeys(
        "Int%param-x%fun-Foo"
    ) {
        """
        fun Foo(x: Int = 1) { print(x) }
        """
    }

    fun testAnnotation() = assertKeys {
        """
        annotation class Foo(val value: Int = 1)
        @Foo fun Bar() {}
        @Foo(2) fun Bam() {}
        """
    }

    // NOTE(lmr): In the future we should try and get this to work
    fun testForLoop() = assertKeys {
        """
        fun Foo() {
            for (x in 0..10) {
                print(x)
            }
        }
        """
    }

    fun testWhileTrue() = assertKeys(
        "Double%arg-1%call-greater%cond%if%body%loop%fun-Foo",
        "Int%arg-0%call-print%body%loop%fun-Foo"
    ) {
        """
        fun Foo() {
            while (true) {
                print(1)
                if (Math.random() > 0.5) break
            }
        }
        """
    }

    fun testWhileCondition() = assertKeys(
        "Int%arg-0%call-print%body%loop%fun-Foo"
    ) {
        """
        fun Foo() {
            while (Math.random() > 0.5) {
                print(1)
            }
        }
        """
    }

    fun testForInCollection() = assertKeys(
        "Int%arg-0%call-print-1%body%loop%fun-Foo"
    ) {
        """
        fun Foo(items: List<Int>) {
            for (item in items) {
                print(item)
                print(1)
            }
        }
        """
    }

    // NOTE(lmr): we should deal with this in some cases, but leaving untouched for now
    fun testConstantProperty() = assertKeys {
        """
        const val foo = 1
        """
    }

    fun testSafeCall() = assertKeys(
        "Boolean%arg-1%call-EQEQ%fun-Foo",
        "String%arg-0%call-contains%else%when%arg-0%call-EQEQ%fun-Foo"
    ) {
        """
        fun Foo(bar: String?): Boolean {
            return bar?.contains("foo") == true
        }
        """
    }

    fun testElvis() = assertKeys(
        "String%branch%when%fun-Foo"
    ) {
        """
        fun Foo(bar: String?): String {
            return bar ?: "Hello World"
        }
        """
    }

    fun testTryCatch() = assertKeys(
        "Int%arg-0%call-invoke%catch%fun-Foo",
        "Int%arg-0%call-invoke%finally%fun-Foo",
        "Int%arg-0%call-invoke%try%fun-Foo"
    ) {
        """
        fun Foo(block: (Int) -> Unit) {
            try {
                block(1)
            } catch(e: Exception) {
                block(2)
            } finally {
                block(3)
            }
        }
        """
    }

    fun testWhen() = assertKeys(
        "Double%arg-1%call-greater%cond%when%fun-Foo",
        "Double%arg-1%call-greater%cond-1%when%fun-Foo",
        "Int%arg-0%call-print%branch%when%fun-Foo",
        "Int%arg-0%call-print%branch-1%when%fun-Foo",
        "Int%arg-0%call-print%else%when%fun-Foo"
    ) {
        """
        fun Foo() {
            when {
                Math.random() > 0.5 -> print(1)
                Math.random() > 0.5 -> print(2)
                else -> print(3)
            }
        }
        """
    }

    fun testWhenWithSubject() = assertKeys(
        "Double%%%this%call-rangeTo%%this%call-contains%cond%when%fun-Foo",
        "Double%%%this%call-rangeTo%%this%call-contains%cond-1%when%fun-Foo",
        "Double%arg-0%call-rangeTo%%this%call-contains%cond%when%fun-Foo",
        "Double%arg-0%call-rangeTo%%this%call-contains%cond-1%when%fun-Foo",
        "Int%arg-0%call-print%branch%when%fun-Foo",
        "Int%arg-0%call-print%branch-1%when%fun-Foo",
        "Int%arg-0%call-print%else%when%fun-Foo"
    ) {
        """
        fun Foo() {
            when (val x = Math.random()) {
                in 0.0..0.5 -> print(1)
                in 0.0..0.2 -> print(2)
                else -> print(3)
            }
        }
        """
    }

    fun testWhenWithSubject2() = assertKeys(
        "Int%arg-0%call-print%branch-1%when%fun-Foo",
        "Int%arg-0%call-print%else%when%fun-Foo",
        "String%arg-0%call-print%branch%when%fun-Foo"
    ) {
        """
        fun Foo(foo: Any) {
            when (foo) {
                is String -> print("Hello World")
                is Int -> print(2)
                else -> print(3)
            }
        }
        """
    }

    fun testDelegatingCtor() = assertKeys(
        "Int%arg-0%call-%init%%class-Bar"
    ) {
        """
        open class Foo(val x: Int)
        class Bar() : Foo(123)
        """
    }

    fun testLocalVal() = assertKeys(
        "Int%arg-0%call-plus%set-y%fun-Foo",
        "Int%val-x%fun-Foo",
        "Int%val-y%fun-Foo"
    ) {
        """
        fun Foo() {
            val x = 1
            var y = 2
            y += 10
        }
        """
    }

    fun testCapturedVar() = assertKeys(
        "Int%val-a%fun-Example",
        "String%0%str%fun-Example",
        "String%2%str%fun-Example"
    ) {
        """
        fun Example(): String {
                val a = 123
                return "foo ${"$"}a bar"
            }
        """
    }

    @Test
    fun testStringTemplate(): Unit = assertKeys(
        "Int%val-a%fun-Example",
        "String%0%str%fun-Example",
        "String%2%str%fun-Example"
    ) {
        """
        fun Example(): String {
            val a = 123
            return "foo ${"$"}a bar"
        }
        """
    }

    @Test
    fun testEnumEntryMultipleArgs(): Unit = assertKeys(
        "Int%arg-0%call-%init%%entry-Bar%class-A",
        "Int%arg-0%call-%init%%entry-Baz%class-A",
        "Int%arg-0%call-%init%%entry-Foo%class-A",
        "Int%arg-1%call-%init%%entry-Bar%class-A",
        "Int%arg-1%call-%init%%entry-Baz%class-A",
        "Int%arg-1%call-%init%%entry-Foo%class-A"
    ) {
        """
        enum class A(val x: Int, val y: Int) {
            Foo(1, 2),
            Bar(2, 3),
            Baz(3, 4)
        }
        """
    }

    fun testCommentsAbove() = assertDurableChange(
        """
            fun Test() {
                print(1)
            }
        """.trimIndent(),
        """
            fun Test() {
                // this is a comment
                print(1)
            }
        """.trimIndent()
    )

    fun testValsAndStructureAbove() = assertDurableChange(
        """
            fun Test() {
                print(1)
            }
        """.trimIndent(),
        """
            fun Test() {
                val x = Math.random()
                println(x)
                print(1)
            }
        """.trimIndent()
    )

    @Test
    fun testBasicTransform(): Unit = assertTransform(
        """
        """,
        """
            fun A() {
              print(1)
              print("Hello World")
              if (true) {
                print(3 + 4)
              }
              if (true) {
                print(1.0f)
              }
              print(3)
            }
        """,
        """
            fun A() {
              print(LiveLiterals%TestKt.Int%arg-0%call-print%fun-A())
              print(LiveLiterals%TestKt.String%arg-0%call-print-1%fun-A())
              if (LiveLiterals%TestKt.Boolean%cond%if%fun-A()) {
                print(LiveLiterals%TestKt.Int%%this%call-plus%arg-0%call-print%branch%if%fun-A() + LiveLiterals%TestKt.Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A())
              }
              if (LiveLiterals%TestKt.Boolean%cond%if-1%fun-A()) {
                print(LiveLiterals%TestKt.Float%arg-0%call-print%branch%if-1%fun-A())
              }
              print(LiveLiterals%TestKt.Int%arg-0%call-print-2%fun-A())
            }
            @LiveLiteralFileInfo(file = "/Test.kt")
            internal class LiveLiterals%TestKt {
              private var Int%arg-0%call-print%fun-A: Int = 1
              private var State%Int%arg-0%call-print%fun-A: State<Int>?
              @LiveLiteralInfo(key = "Int%arg-0%call-print%fun-A", offset = 54)
              fun Int%arg-0%call-print%fun-A(): Int {
                val tmp0 = <this>.State%Int%arg-0%call-print%fun-A
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Int%arg-0%call-print%fun-A", <this>.Int%arg-0%call-print%fun-A)
                  <this>.State%Int%arg-0%call-print%fun-A = tmp1
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              private var String%arg-0%call-print-1%fun-A: String = "Hello World"
              private var State%String%arg-0%call-print-1%fun-A: State<String>?
              @LiveLiteralInfo(key = "String%arg-0%call-print-1%fun-A", offset = 66)
              fun String%arg-0%call-print-1%fun-A(): String {
                val tmp0 = <this>.State%String%arg-0%call-print-1%fun-A
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("String%arg-0%call-print-1%fun-A", <this>.String%arg-0%call-print-1%fun-A)
                  <this>.State%String%arg-0%call-print-1%fun-A = tmp1
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              private var Boolean%cond%if%fun-A: Boolean = true
              private var State%Boolean%cond%if%fun-A: State<Boolean>?
              @LiveLiteralInfo(key = "Boolean%cond%if%fun-A", offset = 86)
              fun Boolean%cond%if%fun-A(): Boolean {
                val tmp0 = <this>.State%Boolean%cond%if%fun-A
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Boolean%cond%if%fun-A", <this>.Boolean%cond%if%fun-A)
                  <this>.State%Boolean%cond%if%fun-A = tmp1
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              private var Int%%this%call-plus%arg-0%call-print%branch%if%fun-A: Int = 3
              private var State%Int%%this%call-plus%arg-0%call-print%branch%if%fun-A: State<Int>?
              @LiveLiteralInfo(key = "Int%%this%call-plus%arg-0%call-print%branch%if%fun-A", offset = 104)
              fun Int%%this%call-plus%arg-0%call-print%branch%if%fun-A(): Int {
                val tmp0 = <this>.State%Int%%this%call-plus%arg-0%call-print%branch%if%fun-A
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Int%%this%call-plus%arg-0%call-print%branch%if%fun-A", <this>.Int%%this%call-plus%arg-0%call-print%branch%if%fun-A)
                  <this>.State%Int%%this%call-plus%arg-0%call-print%branch%if%fun-A = tmp1
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              private var Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A: Int = 4
              private var State%Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A: State<Int>?
              @LiveLiteralInfo(key = "Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A", offset = 108)
              fun Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A(): Int {
                val tmp0 = <this>.State%Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A", <this>.Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A)
                  <this>.State%Int%arg-0%call-plus%arg-0%call-print%branch%if%fun-A = tmp1
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              private var Boolean%cond%if-1%fun-A: Boolean = true
              private var State%Boolean%cond%if-1%fun-A: State<Boolean>?
              @LiveLiteralInfo(key = "Boolean%cond%if-1%fun-A", offset = 121)
              fun Boolean%cond%if-1%fun-A(): Boolean {
                val tmp0 = <this>.State%Boolean%cond%if-1%fun-A
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Boolean%cond%if-1%fun-A", <this>.Boolean%cond%if-1%fun-A)
                  <this>.State%Boolean%cond%if-1%fun-A = tmp1
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              private var Float%arg-0%call-print%branch%if-1%fun-A: Float = 1.0f
              private var State%Float%arg-0%call-print%branch%if-1%fun-A: State<Float>?
              @LiveLiteralInfo(key = "Float%arg-0%call-print%branch%if-1%fun-A", offset = 139)
              fun Float%arg-0%call-print%branch%if-1%fun-A(): Float {
                val tmp0 = <this>.State%Float%arg-0%call-print%branch%if-1%fun-A
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Float%arg-0%call-print%branch%if-1%fun-A", <this>.Float%arg-0%call-print%branch%if-1%fun-A)
                  <this>.State%Float%arg-0%call-print%branch%if-1%fun-A = tmp1
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
              private var Int%arg-0%call-print-2%fun-A: Int = 3
              private var State%Int%arg-0%call-print-2%fun-A: State<Int>?
              @LiveLiteralInfo(key = "Int%arg-0%call-print-2%fun-A", offset = 157)
              fun Int%arg-0%call-print-2%fun-A(): Int {
                val tmp0 = <this>.State%Int%arg-0%call-print-2%fun-A
                return if (tmp0 == null) {
                  val tmp1 = liveLiteral("Int%arg-0%call-print-2%fun-A", <this>.Int%arg-0%call-print-2%fun-A)
                  <this>.State%Int%arg-0%call-print-2%fun-A = tmp1
                  tmp1
                } else {
                  tmp0
                }
                .value
              }
            }
        """
    )

    private var builtKeys = mutableSetOf<String>()

    override fun postProcessingStep(
        module: IrModuleFragment,
        generatorContext: GeneratorContext,
        irProviders: List<IrProvider>
    ) {
        val pluginContext = IrPluginContext(
            generatorContext.moduleDescriptor,
            generatorContext.bindingContext,
            generatorContext.languageVersionSettings,
            generatorContext.symbolTable,
            generatorContext.typeTranslator,
            generatorContext.irBuiltIns,
            irProviders = irProviders
        )
        val bindingTrace = DelegatingBindingTrace(pluginContext.bindingContext, "test trace")
        val symbolRemapper = DeepCopySymbolRemapper()
        val keyVisitor = DurableKeyVisitor(builtKeys)
        val transformer = object : LiveLiteralTransformer(
            true,
            keyVisitor,
            pluginContext,
            symbolRemapper,
            bindingTrace
        ) {
            override fun makeKeySet(): MutableSet<String> {
                return super.makeKeySet().also { builtKeys = it }
            }
        }
        transformer.lower(module)
    }

    // since the lowering will throw an exception if duplicate keys are found, all we have to do
    // is run the lowering
    private fun assertNoDuplicateKeys(src: String) {
        generateIrModuleWithJvmResolve(
            listOf(
                sourceFile("Test.kt", src.replace('%', '$'))
            )
        )
    }

    // For a given src string, a
    private fun assertKeys(vararg keys: String, makeSrc: () -> String) {
        builtKeys = mutableSetOf()
        generateIrModuleWithJvmResolve(
            listOf(
                sourceFile("Test.kt", makeSrc().replace('%', '$'))
            )
        )
        assertEquals(
            keys.toList().sorted().joinToString(separator = ",\n") {
                "\"${it.replace('$', '%')}\""
            },
            builtKeys.toList().sorted().joinToString(separator = ",\n") {
                "\"${it.replace('$', '%')}\""
            }
        )
    }

    // test: have two src strings (before/after) and assert that the keys of the params didn't change
    private fun assertDurableChange(before: String, after: String) {
        generateIrModuleWithJvmResolve(
            listOf(
                sourceFile("Test.kt", before.replace('%', '$'))
            )
        )
        val beforeKeys = builtKeys

        builtKeys = mutableSetOf()

        generateIrModuleWithJvmResolve(
            listOf(
                sourceFile("Test.kt", after.replace('%', '$'))
            )
        )
        val afterKeys = builtKeys

        assertEquals(
            beforeKeys.toList().sorted().joinToString(separator = "\n"),
            afterKeys.toList().sorted().joinToString(separator = "\n")
        )
    }

    private fun assertTransform(
        unchecked: String,
        checked: String,
        expectedTransformed: String,
        dumpTree: Boolean = false
    ) = verifyComposeIrTransform(
        """
            import androidx.compose.Composable
            $checked
        """.trimIndent(),
        expectedTransformed,
        """
            import androidx.compose.Composable
            $unchecked
        """.trimIndent(),
        dumpTree
    )
}