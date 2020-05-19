// FILE: test.kt

enum class E {
    E1;

    fun foo() = {
        prop
    }

    val prop = 22
}

fun box() {
    E.E1.foo()
}

// LINENUMBERS
// test.kt:14 box
// test.kt:6 foo
// test.kt:8 foo
// test.kt:14 box
// test.kt:15 box
