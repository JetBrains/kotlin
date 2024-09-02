
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

// EXPECTATIONS JVM_IR
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

// EXPECTATIONS WASM
// test.kt:15 $box (5, 20, 20, 20)
// test.kt:15 $<no name provided>.<init>
// test.kt:8 $A.bar (15, 15, 15, 8, 15, 15, 15, 8)
// test.kt:5 $A.foo (16, 18, 16, 18)
// test.kt:16 $box (4, 8, 8, 8)
// test.kt:12 $B.<init>
// test.kt:17 $box
