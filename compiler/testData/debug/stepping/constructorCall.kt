// FILE: test.kt

class A(val x: Int)

fun box() {
    A(1)
    A(
        2
    )
    A(3
    )
    A(
        4)
}

// EXPECTATIONS JVM JVM_IR
// test.kt:6 box
// test.kt:3 <init>
// test.kt:6 box
// test.kt:7 box
// test.kt:8 box
// test.kt:7 box
// test.kt:3 <init>
// test.kt:7 box
// test.kt:10 box
// test.kt:3 <init>
// test.kt:10 box
// test.kt:12 box
// test.kt:13 box
// test.kt:12 box
// test.kt:3 <init>
// test.kt:12 box
// test.kt:14 box

// EXPECTATIONS JS_IR
// test.kt:6 box
// test.kt:3 <init>
// test.kt:3 <init>
// test.kt:7 box
// test.kt:3 <init>
// test.kt:3 <init>
// test.kt:10 box
// test.kt:3 <init>
// test.kt:3 <init>
// test.kt:12 box
// test.kt:3 <init>
// test.kt:3 <init>
// test.kt:14 box