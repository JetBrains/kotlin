// FILE: test.kt

fun box() {
    foo(
            1 + 1
    )
}

fun foo(i: Int) {
}

// LINENUMBERS
// test.kt:5 box
// test.kt:4 box
// test.kt:10 foo
// test.kt:7 box
