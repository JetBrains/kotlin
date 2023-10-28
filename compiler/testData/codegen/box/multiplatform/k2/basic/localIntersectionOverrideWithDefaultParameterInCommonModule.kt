// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, WASM
// IGNORE_NATIVE_K1: mode=ONE_STAGE_MULTI_MODULE
// ISSUE: KT-60850
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect class Expect {
    fun foo(): String
}

interface Base {
    fun cancel(s: Expect? = null): String
}

open class Derived : Base {
    override fun cancel(s: Expect?): String {
        return s?.foo() ?: "OK"
    }
}

open class AbstractImpl : Derived(), Base

fun testCommon(): String {
    class LocalCommon : AbstractImpl() {
        fun test(): String {
            return cancel()
        }
    }
    return LocalCommon().test()
}

// MODULE: platform()()(common)
// FILE: platform.kt

class ActualTarget {
    fun foo(): String = "Fail"
}

fun testPlatform(): String {
    class LocalPlatform : AbstractImpl() {
        fun test(): String {
            return cancel()
        }
    }
    return LocalPlatform().test()
}

actual typealias Expect = ActualTarget

fun box(): String {
    if (testCommon() != "OK") return "Fail"
    return testPlatform()
}
