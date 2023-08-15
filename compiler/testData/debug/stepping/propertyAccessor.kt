// IGNORE_BACKEND: WASM
// FILE: test.kt

class A {
    val prop : Int
        get() {
            return 1
        }
}

fun box() {
    A().prop
}

// EXPECTATIONS JVM JVM_IR
// test.kt:12 box
// test.kt:4 <init>
// test.kt:12 box
// test.kt:7 getProp
// test.kt:12 box
// test.kt:13 box

// EXPECTATIONS JS_IR
// test.kt:12 box
// test.kt:4 <init>
// test.kt:12 box
// test.kt:7 <get-prop>
// test.kt:13 box