// IGNORE_BACKEND: JS_IR
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

// EXPECTATIONS JVM JVM_IR
// test.kt:5 box
// test.kt:15 box
// test.kt:16 box
// test.kt:6 box
// test.kt:7 box
// test.kt:16 box
// test.kt:17 box
// test.kt:9 box
// test.kt:15 box
// test.kt:16 box
// test.kt:10 box
// test.kt:11 box
// test.kt:16 box
// test.kt:17 box
// test.kt:12 box

// EXPECTATIONS JS_IR
