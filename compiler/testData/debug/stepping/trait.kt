// IGNORE_BACKEND: WASM
// FILE: test.kt

interface A {
    fun foo() = 32

    fun bar(): Int {
        return foo()
    }
}

class B : A

fun box() {
    (object : A {}).bar()
    B().bar()
}

// The dispatch methods added to classes directly implementing
// interfaces with default methods (forwarding to the actual implementation
// on A$DefaultImpls) have the line number of the class declaration.

// EXPECTATIONS JVM JVM_IR
// test.kt:15 box
// test.kt:15 <init>
// test.kt:15 box
// test.kt:15 bar
// test.kt:8 bar
// test.kt:15 foo
// test.kt:5 foo
// test.kt:15 foo
// test.kt:8 bar
// test.kt:15 bar
// test.kt:15 box
// test.kt:16 box
// test.kt:12 <init>
// test.kt:16 box
// test.kt:12 bar
// test.kt:8 bar
// test.kt:12 foo
// test.kt:5 foo
// test.kt:12 foo
// test.kt:8 bar
// test.kt:12 bar
// test.kt:16 box
// test.kt:17 box

// EXPECTATIONS JS_IR
// test.kt:15 box
// test.kt:15 <init>
// test.kt:15 box
// test.kt:8 bar
// test.kt:5 foo
// test.kt:16 box
// test.kt:12 <init>
// test.kt:16 box
// test.kt:8 bar
// test.kt:5 foo
// test.kt:17 box