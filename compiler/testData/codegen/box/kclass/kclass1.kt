/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// Unsupported [This reflection API is not supported yet in JavaScript]
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM
// WITH_STDLIB

package codegen.kclass.kclass1

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    App(testQualified = true)
    return sb.toString()
}

// Taken from:
// https://github.com/SalomonBrys/kmffkn/blob/master/shared/main/kotlin/com/github/salomonbrys/kmffkn/app.kt

@DslMarker
annotation class MyDsl

@MyDsl
class DslMain {
    fun <T: Any> kClass(block: KClassDsl.() -> T): T = KClassDsl().block()
}

@MyDsl
class KClassDsl {
    inline fun <reified T: Any> of() = T::class
}

fun <T: Any> dsl(block: DslMain.() -> T): T = DslMain().block()

class TestClass

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
class App(testQualified: Boolean) {

    var type = dsl {
        kClass {
            //kClass {  } // This should error if uncommented because of `@DslMarker`.
            of<TestClass>()
        }
    }

    init {
        assertTrue(type.simpleName == "TestClass")
        if (testQualified)
            assertTrue(type.qualifiedName == "codegen.kclass.kclass1.TestClass") // This is not really necessary, but always better :).

        assertTrue(String::class == String::class)
        assertTrue(String::class != Int::class)

        assertTrue(TestClass()::class == TestClass()::class)
        assertTrue(TestClass()::class == TestClass::class)

        sb.append("OK")
    }
}
