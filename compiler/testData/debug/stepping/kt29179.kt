// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JVM_IR
// FILE: test.kt
class A {
    val a = 1
    fun bar() = 2
    fun foo() {
        3
            //Breakpoint! from the Evaluate Expression test suite.
            .toString()
    }
}

fun box() {
    A().foo()
}

// EXPECTATIONS JVM JVM_IR
// test.kt:15 box
// test.kt:4 <init>
// test.kt:5 <init>
// test.kt:15 box
// test.kt:8 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:16 box

// EXPECTATIONS JS_IR
// test.kt:15 box
// test.kt:5 <init>
// test.kt:4 <init>
// test.kt:15 box
// test.kt:11 foo
// test.kt:16 box