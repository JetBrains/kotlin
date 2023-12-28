
// FILE: test.kt

class A

fun bar(a: A) = A()

fun box() {
    val a = A()
    bar(
            bar(
                    bar(a)
            )
    )
}

// EXPECTATIONS JVM_IR
// test.kt:9 box
// test.kt:4 <init>
// test.kt:9 box
// test.kt:12 box
// test.kt:6 bar
// test.kt:4 <init>
// test.kt:6 bar
// test.kt:11 box
// test.kt:6 bar
// test.kt:4 <init>
// test.kt:6 bar
// test.kt:10 box
// test.kt:6 bar
// test.kt:4 <init>
// test.kt:6 bar
// test.kt:10 box
// test.kt:15 box

// EXPECTATIONS JS_IR
// test.kt:9 box
// test.kt:4 <init>
// test.kt:10 box
// test.kt:6 bar
// test.kt:4 <init>
// test.kt:11 box
// test.kt:6 bar
// test.kt:4 <init>
// test.kt:10 box
// test.kt:6 bar
// test.kt:4 <init>
// test.kt:15 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:9 $box (12, 12)
// test.kt:4 $A.<init> (7, 7, 7, 7)
// test.kt:12 $box (24, 20)
// test.kt:6 $bar (16, 16, 19, 16, 16, 19, 16, 16, 19)
// test.kt:11 $box

// test.kt:10 $box (4, 4)
// test.kt:15 $box
