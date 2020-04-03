//FILE: test.kt
fun box() {
    for (i in 1..3) {
        foo(i)
    }
}

inline fun foo(n: Int) {}

// LINENUMBERS
// test.kt:3
// test.kt:4
// test.kt:8
// test.kt:3
// test.kt:4
// test.kt:8
// test.kt:3
// test.kt:4
// test.kt:8
// test.kt:3
// test.kt:6