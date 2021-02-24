// FILE: test.kt

fun box() {
    foo({
            val a = 1
        })

    foo() {
        val a = 1
    }
}

inline fun foo(f: () -> Unit) {
    val a = 1
    f()
}

// LINENUMBERS
// test.kt:4 box
// test.kt:14 box
// test.kt:15 box
// test.kt:5 box
// test.kt:6 box
// test.kt:16 box
// test.kt:8 box
// test.kt:14 box
// test.kt:15 box
// test.kt:9 box
// test.kt:10 box
// test.kt:16 box
// test.kt:11 box
