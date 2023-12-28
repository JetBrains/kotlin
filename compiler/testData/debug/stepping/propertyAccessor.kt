
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

// EXPECTATIONS JVM_IR
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

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:12 $box (4, 4, 8, 8)
// test.kt:9 $A.<init>
// test.kt:7 $A.<get-prop> (19, 12)
// test.kt:13 $box
