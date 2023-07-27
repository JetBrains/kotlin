// IGNORE_BACKEND_K2: WASM
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

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:8 <init>
// test.kt:5 box
// test.kt:10 <init>
// test.kt:5 box
// test.kt:15 test
// test.kt:16 test
// test.kt:10 getA
// test.kt:16 test
// test.kt:11 invoke
// test.kt:17 test
// test.kt:15 test
// test.kt:18 test
// test.kt:6 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:8 <init>
// test.kt:5 box
// test.kt:10 <init>
// test.kt:10 <init>
// test.kt:5 box
// test.kt:11 invoke
// test.kt:18 test
// test.kt:6 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:5 $box (9, 11, 11, 9, 4)
// test.kt:8 $A.<init>
// test.kt:10 $B.<init>
// test.kt:12 $B.<init>
// test.kt:15 $test (4, 4)
// Standard.kt:3 $test (77, 90)
// Standard.kt:67 $test
// Standard.kt:70 $test (20, 11, 20, 4)
// test.kt:11 $B.invoke
// test.kt:18 $test
// test.kt:6 $box
