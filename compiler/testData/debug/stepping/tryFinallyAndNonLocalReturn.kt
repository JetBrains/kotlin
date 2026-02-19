// TARGET_BACKEND: JVM


// FILE: test.kt

fun finallyAction() {}

inline fun <R> analyze(
    action: () -> R,
): R {
    return try {
        action()
    } finally {
        finallyAction()
    }
}


private fun callSite(flag: Boolean) {
    analyze {
        foo(flag) ?: return // <-- non-local return
        42
    }
}

fun foo(flag: Boolean): Int? = if (flag) 42 else null

fun box() {
    callSite(true)
    callSite(false)
}

// EXPECTATIONS JVM_IR
// test.kt:29 box
// test.kt:20 callSite
// test.kt:11 callSite
// test.kt:12 callSite
// test.kt:21 callSite
// test.kt:26 foo
// test.kt:21 callSite
// test.kt:22 callSite
// test.kt:12 callSite
// test.kt:14 callSite
// test.kt:6 finallyAction
// test.kt:15 callSite
// test.kt:11 callSite
// test.kt:24 callSite
// test.kt:30 box
// test.kt:20 callSite
// test.kt:11 callSite
// test.kt:12 callSite
// test.kt:21 callSite
// test.kt:26 foo
// test.kt:21 callSite
// test.kt:14 callSite
// test.kt:6 finallyAction
// test.kt:14 callSite
// test.kt:31 box
