// FILE: test.kt

class A {
    fun f(flag: Boolean) {
        if (flag) {
            toString()
        }
        super.toString()
    }
}

fun box() {
    val a = A()
    a.f(false)
}

// EXPECTATIONS JVM_IR
// test.kt:13 box
// test.kt:3 <init>
// test.kt:13 box
// test.kt:14 box
// test.kt:5 f
// test.kt:8 f
// test.kt:9 f
// test.kt:15 box

// EXPECTATIONS NATIVE
// test.kt:13 box
// test.kt:3 <init>
// test.kt:10 <init>
// test.kt:13 box
// test.kt:14 box
// test.kt:4 f
// test.kt:5 f
// test.kt:8 f
// test.kt:9 f
// test.kt:15 box

// EXPECTATIONS JS_IR
// test.kt:13 box
// test.kt:3 <init>
// test.kt:14 box
// test.kt:5 f
// test.kt:8 f
// test.kt:9 f
// test.kt:15 box

// EXPECTATIONS WASM
// test.kt:13 $box (12)
// test.kt:10 $A.<init> (1)
// test.kt:14 $box (4, 8, 6)
// test.kt:5 $A.f (12)
// test.kt:8 $A.f (8, 14)
// test.kt:9 $A.f (5)
// test.kt:15 $box (1)
