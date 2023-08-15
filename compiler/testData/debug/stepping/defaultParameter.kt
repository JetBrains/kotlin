// IGNORE_BACKEND: WASM
// FILE: test.kt

class A {
    fun computeParam() = 32

    fun foo(param: Int = computeParam()) {
    }
}

fun box() {
    A().foo()
}

// FORCE_STEP_INTO
// EXPECTATIONS JVM JVM_IR
// test.kt:12 box
// test.kt:4 <init>
// test.kt:12 box
// test.kt:7 foo$default (synthetic)
// test.kt:5 computeParam
// test.kt:7 foo$default (synthetic)
// test.kt:8 foo
// test.kt:7 foo$default (synthetic)
// test.kt:13 box

// EXPECTATIONS JS_IR
// test.kt:12 box
// test.kt:4 <init>
// test.kt:12 box
// test.kt:7 foo$default
// test.kt:5 computeParam
// test.kt:8 foo
// test.kt:13 box
