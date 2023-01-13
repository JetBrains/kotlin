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
// test.kt:11 box
// test.kt:3 <init>
// test.kt:11 box
// test.kt:6 foo$default (synthetic)
// test.kt:4 computeParam
// test.kt:6 foo$default (synthetic)
// test.kt:7 foo
// test.kt:6 foo$default (synthetic)
// test.kt:12 box

// EXPECTATIONS JS_IR
// test.kt:11 box
// test.kt:3 <init>
// test.kt:11 box
// test.kt:6 foo$default
// test.kt:4 computeParam
// test.kt:7 foo
// test.kt:12 box
