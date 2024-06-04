// ISSUE: KT-68801
// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JVM_IR JS_IR JS_IR_ES6 WASM NATIVE
// ^^^ This test fails with `java.lang.IllegalArgumentException: There should be no references to expect classes at this point`
//     at org.jetbrains.kotlin.backend.common.actualizer.SpecialFakeOverrideSymbolsResolver.processClass(SpecialFakeOverrideSymbolsResolver.kt:97)

// MODULE: common
// FILE: common.kt
open expect class A() {
    fun foo(): String
}

expect class B() : A

fun test() = B().foo()

// MODULE: platform()()(common)
// FILE: platform.kt
open class Base {
    fun foo() = "OK"
}

actual open class A : Base()

actual class B : A()

fun box() : String {
    return test()
}