// TARGET_BACKEND: JVM


// FILE: test.kt

fun finallyAction() {}

inline fun <R> analyze(
    action: () -> R,
): R {
    return try { val result = action()
        result } finally { finallyAction() }
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
// test.kt:25 box
// test.kt:16 callSite
// test.kt:11 callSite
// test.kt:17 callSite
// test.kt:22 foo
// test.kt:17 callSite
// test.kt:18 callSite
// test.kt:11 callSite
// test.kt:12 callSite
// test.kt:6 finallyAction
// test.kt:12 callSite
// test.kt:11 callSite
// test.kt:20 callSite
// test.kt:26 box
// test.kt:16 callSite
// test.kt:11 callSite
// test.kt:17 callSite
// test.kt:22 foo
// test.kt:17 callSite
// test.kt:12 callSite
// test.kt:6 finallyAction
// test.kt:12 callSite
// test.kt:27 box
