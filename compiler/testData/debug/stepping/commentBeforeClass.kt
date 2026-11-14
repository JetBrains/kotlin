
// FILE: test.kt

fun box() {
    A()
}

// Some comment
class A {

}

// EXPECTATIONS JVM
// test.kt:5 box
// test.kt:9 <init>
// test.kt:5 box
// test.kt:6 box

// EXPECTATIONS NATIVE
// test.kt:5 box
// test.kt:9 <init>
// test.kt:11 <init>
// test.kt:6 box

// EXPECTATIONS JS_IR JS_IR_ES6
// test.kt:5 box
// EXPECTATIONS JS_IR
// test.kt:9 <init>
// EXPECTATIONS JS_IR JS_IR_ES6
// test.kt:6 box

// EXPECTATIONS WASM_JS
// test.kt:5 $box (4)
// test.kt:11 $A.<init> (1)
// test.kt:5 $box (4)
// test.kt:6 $box (1)
