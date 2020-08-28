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

// LINENUMBERS
// test.kt:14 box
// test.kt:14 <init>
// test.kt:14 box
// test.kt:14 bar
// test.kt:7 bar
// test.kt:14 foo
// test.kt:4 foo
// test.kt:14 foo
// test.kt:7 bar
// test.kt:14 bar
// test.kt:14 box
// test.kt:15 box
// test.kt:11 <init>
// test.kt:15 box
// test.kt:11 bar
// test.kt:7 bar
// test.kt:11 foo
// test.kt:4 foo
// test.kt:11 foo
// test.kt:7 bar
// test.kt:11 bar
// test.kt:15 box
// test.kt:16 box