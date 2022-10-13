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

// EXPECTATIONS JVM JVM_IR
// test.kt:8 box
// test.kt:3 <init>
// test.kt:8 box
// test.kt:11 box
// test.kt:5 bar
// test.kt:3 <init>
// test.kt:5 bar
// test.kt:10 box
// test.kt:5 bar
// test.kt:3 <init>
// test.kt:5 bar
// test.kt:9 box
// test.kt:5 bar
// test.kt:3 <init>
// test.kt:5 bar
// test.kt:9 box
// test.kt:14 box

// EXPECTATIONS JS_IR
// test.kt:8 box
// test.kt:3 <init>
// test.kt:9 box
// test.kt:5 bar
// test.kt:3 <init>
// test.kt:10 box
// test.kt:5 bar
// test.kt:3 <init>
// test.kt:9 box
// test.kt:5 bar
// test.kt:3 <init>
// test.kt:14 box