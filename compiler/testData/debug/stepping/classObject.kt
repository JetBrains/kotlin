
// FILE: test.kt

class A {
    companion object {
        val prop0 = 1
        val prop1 = 2
        fun foo(): Int {
            return prop0 + prop1
        }
    }
}

fun box() {
    A.prop0
    A.prop1
    A.foo()
}

// EXPECTATIONS JVM_IR
// test.kt:15 box
// test.kt:6 <clinit>
// test.kt:7 <clinit>
// test.kt:6 getProp0
// test.kt:6 getProp0
// test.kt:15 box
// test.kt:16 box
// test.kt:7 getProp1
// test.kt:7 getProp1
// test.kt:16 box
// test.kt:17 box
// test.kt:9 foo
// test.kt:6 getProp0
// test.kt:6 getProp0
// test.kt:9 foo
// test.kt:7 getProp1
// test.kt:7 getProp1
// test.kt:9 foo
// test.kt:17 box
// test.kt:18 box

// EXPECTATIONS JS_IR
// test.kt:15 box
// test.kt:6 <init>
// test.kt:7 <init>
// test.kt:5 <init>
// test.kt:16 box
// test.kt:17 box
// test.kt:17 box
// test.kt:9 foo
// test.kt:18 box

// EXPECTATIONS WASM
// test.kt:15 $box (6, 6)
// test.kt:16 $box (6, 6)
// test.kt:17 $box
// test.kt:9 $Companion.foo (19, 19, 27, 27, 19, 12)
// test.kt:18 $box
