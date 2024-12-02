
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
// EXPECTATIONS JVM_IR
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

// EXPECTATIONS WASM
// test.kt:11 $box (10)
// test.kt:12 $box (4, 8)
// test.kt:4 $A.<init> (0)
// test.kt:9 $A.<init> (1)
// test.kt:5 $A.computeParam (25)
// test.kt:7 $A.foo (41)
// test.kt:8 $A.foo (5)
// test.kt:13 $box (1)
