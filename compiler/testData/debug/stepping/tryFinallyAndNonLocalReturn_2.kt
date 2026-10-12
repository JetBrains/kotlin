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

fun bar(x: Any?): Int {
    try {
        val abc = run {
            x ?: return 123
            42
        }
        return abc
    } finally {
        finallyAction()
    }
}

fun box() {
    foo("nonnull")
    bar("nonnull")
    foo(null)
    bar(null)
}

// EXPECTATIONS JVM
// test.kt:30 box
// test.kt:7 foo
// test.kt:8 foo
// test.kt:9 foo
// test.kt:10 foo
// test.kt:8 foo
// test.kt:13 foo
// test.kt:4 finallyAction
// test.kt:14 foo
// test.kt:15 foo
// test.kt:31 box
// test.kt:18 bar
// test.kt:19 bar
// test.kt:20 bar
// test.kt:21 bar
// test.kt:19 bar
// test.kt:23 bar
// test.kt:25 bar
// test.kt:4 finallyAction
// test.kt:25 bar
// test.kt:23 bar
// test.kt:31 box
// test.kt:32 box
// test.kt:7 foo
// test.kt:8 foo
// test.kt:9 foo
// test.kt:13 foo
// test.kt:4 finallyAction
// test.kt:9 foo
// test.kt:33 box
// test.kt:18 bar
// test.kt:19 bar
// test.kt:20 bar
// test.kt:25 bar
// test.kt:4 finallyAction
// test.kt:25 bar
// test.kt:20 bar
// test.kt:33 box
// test.kt:34 box
