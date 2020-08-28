// FILE: test.kt

class A {
    fun foo() = this
    inline fun bar() = this
}

fun box() {
    val a = A()
    a
     .foo()

    a
     .bar()
}

// LINENUMBERS
// test.kt:9 box
// test.kt:3 <init>
// test.kt:9 box
// test.kt:10 box
// test.kt:11 box
// test.kt:4 foo
// test.kt:11 box
// test.kt:13 box
// test.kt:14 box
// test.kt:5 box
// test.kt:15 box
