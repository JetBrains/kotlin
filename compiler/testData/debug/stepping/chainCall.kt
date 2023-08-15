// IGNORE_BACKEND: WASM
// FILE: test.kt

class A {
    fun foo() = this
    inline fun bar() = this
}

fun box() {
    val a = A()
    a.foo()
        .foo()

    a.bar()
        .bar()
}

// EXPECTATIONS JVM JVM_IR
// test.kt:10 box
// test.kt:4 <init>
// test.kt:10 box
// test.kt:11 box
// test.kt:5 foo
// test.kt:12 box
// test.kt:5 foo
// test.kt:12 box
// test.kt:14 box
// test.kt:6 box
// test.kt:15 box
// test.kt:6 box
// test.kt:16 box

// EXPECTATIONS JS_IR
// test.kt:10 box
// test.kt:4 <init>
// test.kt:11 box
// test.kt:5 foo
// test.kt:12 box
// test.kt:5 foo
// test.kt:16 box
