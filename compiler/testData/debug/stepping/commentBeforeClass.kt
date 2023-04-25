// FILE: test.kt

fun box() {
    A()
}

// Some comment
class A {

}

// EXPECTATIONS JVM JVM_IR
// test.kt:4 box
// test.kt:8 <init>
// test.kt:4 box
// test.kt:5 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:8 <init>
// test.kt:5 box
