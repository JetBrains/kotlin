// IGNORE_BACKEND: WASM
// FILE: test.kt

class A {
    val prop = 1

    fun foo() {
        prop
    }
}

fun box() {
    val a = A()
    a.prop
    a.foo()
}

// EXPECTATIONS JVM_IR
// test.kt:13 box
// test.kt:4 <init>
// test.kt:5 <init>
// test.kt:4 <init>
// test.kt:13 box
// test.kt:14 box
// test.kt:5 getProp
// test.kt:14 box
// test.kt:15 box
// test.kt:8 foo
// test.kt:9 foo
// test.kt:16 box

// EXPECTATIONS JS_IR
// test.kt:13 box
// test.kt:5 <init>
// test.kt:4 <init>
// test.kt:15 box
// test.kt:9 foo
// test.kt:16 box
