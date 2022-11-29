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
// test.kt:11 box
// test.kt:3 <init>
// test.kt:11 box
// test.kt:6 getProp
// test.kt:11 box
// test.kt:12 box

// EXPECTATIONS JS_IR
// test.kt:11 box
// test.kt:3 <init>
// test.kt:11 box
// test.kt:6 <get-prop>
// test.kt:12 box