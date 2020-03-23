// FILE: test.kt
inline fun f(x: () -> Unit) = x()
inline fun g() {}

fun box() {
    f {
        g()
        g()
    }
}

// LINENUMBERS
// test.kt:6
// test.kt:2 @ test.kt:6
// test.kt:7
// test.kt:3 @ test.kt:7
// test.kt:8
// test.kt:3 @ test.kt:8
// test.kt:9
// test.kt:10

// lines 3 have incorrect call sites (KT-35006)
// IGNORE_BACKEND: JVM, JVM_IR