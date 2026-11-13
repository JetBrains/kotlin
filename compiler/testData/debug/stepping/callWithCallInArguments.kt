
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

// EXPECTATIONS JVM
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

// EXPECTATIONS NATIVE
// test.kt:9 box
// test.kt:4 <init>
// test.kt:9 box
// test.kt:12 box
// test.kt:6 bar
// test.kt:4 <init>
// test.kt:6 bar
// test.kt:12 box
// test.kt:11 box
// test.kt:6 bar
// test.kt:4 <init>
// test.kt:6 bar
// test.kt:11 box
// test.kt:10 box
// test.kt:6 bar
// test.kt:4 <init>
// test.kt:6 bar
// test.kt:15 box

// EXPECTATIONS JS_IR JS_IR_ES6
// test.kt:9 box
// EXPECTATIONS JS_IR
// test.kt:4 <init>
// EXPECTATIONS JS_IR JS_IR_ES6
// test.kt:10 box
// test.kt:6 bar
// EXPECTATIONS JS_IR
// test.kt:4 <init>
// EXPECTATIONS JS_IR JS_IR_ES6
// test.kt:11 box
// test.kt:6 bar
// EXPECTATIONS JS_IR
// test.kt:4 <init>
// EXPECTATIONS JS_IR JS_IR_ES6
// test.kt:10 box
// test.kt:6 bar
// EXPECTATIONS JS_IR
// test.kt:4 <init>
// EXPECTATIONS JS_IR JS_IR_ES6
// test.kt:15 box

// EXPECTATIONS WASM_JS
// test.kt:9 $box (12)
// test.kt:4 $A.<init> (7)
// test.kt:12 $box (24, 20)
// test.kt:6 $bar (16)
// test.kt:4 $A.<init> (7)
// test.kt:6 $bar (19)
// test.kt:11 $box (12)
// test.kt:6 $bar (16)
// test.kt:4 $A.<init> (7)
// test.kt:6 $bar (19)
// test.kt:10 $box (4)
// test.kt:6 $bar (16)
// test.kt:4 $A.<init> (7)
// test.kt:6 $bar (19)
// test.kt:10 $box (4)
// test.kt:15 $box (1)
