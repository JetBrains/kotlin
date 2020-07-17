// FILE: test.kt

class A {
    val prop : Int
        get() {
            return 1
        }
}

fun box() {
    A().prop
}

// LINENUMBERS
// test.kt:11 box
// test.kt:3 <init>
// test.kt:11 box
// test.kt:6 getProp
// test.kt:11 box
// test.kt:12 box
