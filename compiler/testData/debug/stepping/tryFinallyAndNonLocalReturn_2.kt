// TARGET_BACKEND: JVM
// FILE: test.kt

fun finallyAction() {}

fun foo(x: Any?) {
    try {
        val abc = run {
            x ?: return
            42
        }
    } finally {
        finallyAction()
    }
}

fun box() {
    foo("nonnull")
}

// EXPECTATIONS JVM
// test.kt:18 box
// test.kt:7 foo
// test.kt:8 foo
// test.kt:9 foo
// test.kt:10 foo
// test.kt:8 foo
// test.kt:13 foo
// test.kt:4 finallyAction
// test.kt:14 foo
// test.kt:15 foo
// test.kt:19 box
