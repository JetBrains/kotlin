// FILE: test.kt

fun box() {
    test(B(A()))
}

class A

class B(val a: A) {
    operator fun A.invoke() {}
}

fun test(b: B) {
    with(b) {
        a()
    }
}

// EXPECTATIONS JVM JVM_IR
// test.kt:4 box
// test.kt:7 <init>
// test.kt:4 box
// test.kt:9 <init>
// test.kt:4 box
// test.kt:14 test

// test.kt:15 test
// test.kt:9 getA
// test.kt:15 test
// test.kt:10 invoke
// test.kt:16 test
// test.kt:14 test
// test.kt:17 test
// test.kt:5 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:7 <init>
// test.kt:4 box
// test.kt:9 <init>
// test.kt:9 <init>
// test.kt:4 box
// test.kt:10 invoke
// test.kt:17 test
// test.kt:5 box
