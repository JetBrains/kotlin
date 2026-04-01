
// FILE: test.kt

annotation class Anno {
    annotation class Child
}

@Anno
@Anno.Child
fun foo(): String = "O"

class C() {
    @Anno
    @Anno.Child
    fun foo(): String = "K"
}

fun box(): String =
    foo() +
            C().foo()

// EXPECTATIONS JVM_IR
// test.kt:19 box
// test.kt:10 foo
// test.kt:19 box
// test.kt:20 box
// test.kt:12 <init>
// test.kt:20 box
// test.kt:15 foo
// test.kt:19 box
// test.kt:20 box

// EXPECTATIONS NATIVE
// test.kt:19 box
// test.kt:10 foo
// test.kt:19 box
// test.kt:20 box
// test.kt:12 <init>
// test.kt:20 box
// test.kt:15 foo
// test.kt:20 box
// test.kt:19 box
// test.kt:19 box
// test.kt:20 box

// EXPECTATIONS JS_IR
// test.kt:20 box
// test.kt:10 foo
// test.kt:20 box
// test.kt:12 <init>
// test.kt:20 box
// test.kt:15 foo

// EXPECTATIONS WASM
// test.kt:19 $box (4)
// test.kt:10 $foo (20, 23)
// test.kt:20 $box (12)
// test.kt:12 $C.<init> (9)
// test.kt:20 $box (16)
// test.kt:15 $C.foo (24, 27)
// test.kt:19 $box (4)
// test.kt:20 $box (21)
