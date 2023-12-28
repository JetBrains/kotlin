
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

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:13 $box (12, 12)
// test.kt:5 $A.<init>
// test.kt:10 $A.<init>
// test.kt:14 $box (4, 6)
// test.kt:15 $box (4, 6)
// test.kt:8 $A.foo
// test.kt:9 $A.foo
// test.kt:16 $box
