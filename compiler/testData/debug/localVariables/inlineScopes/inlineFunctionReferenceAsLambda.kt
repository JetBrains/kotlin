// TARGET_BACKEND: JVM
// FILE: test.kt
// KT-91571: a callable reference to an inline function passed as an inline lambda is inlined like a lambda,
// it has no function marker of its own.

inline fun <T> identity(x: T): T {
    val y = x
    return y
}

inline fun <A, R> fold(a: A, f: (A) -> R): R {
    val before = a
    return f(before)
}

fun box() {
    fold(1, ::identity)
}

// EXPECTATIONS JVM +USE_INLINE_SCOPES_NUMBERS
// test.kt:17 box:
// test.kt:12 box: a\1:int=1:int, $i$f$fold\1\17:int=0:int
// test.kt:13 box: a\1:int=1:int, $i$f$fold\1\17:int=0:int, before\1:int=1:int
// test.kt:17 box: a\1:int=1:int, $i$f$fold\1\17:int=0:int, before\1:int=1:int, p0\2:int=1:int, $i$a$-fold-TestKt$box$1\2\41\0:int=0:int
// test.kt:7 box: a\1:int=1:int, $i$f$fold\1\17:int=0:int, before\1:int=1:int, p0\2:int=1:int, $i$a$-fold-TestKt$box$1\2\41\0:int=0:int, x\3:int=1:int, $i$f$identity\3\17:int=0:int
// test.kt:8 box: a\1:int=1:int, $i$f$fold\1\17:int=0:int, before\1:int=1:int, p0\2:int=1:int, $i$a$-fold-TestKt$box$1\2\41\0:int=0:int, x\3:int=1:int, $i$f$identity\3\17:int=0:int, y\3:int=1:int
// test.kt:13 box: a\1:int=1:int, $i$f$fold\1\17:int=0:int, before\1:int=1:int
// test.kt:18 box:

// EXPECTATIONS JVM
// test.kt:17 box:
// test.kt:12 box: a$iv:int=1:int, $i$f$fold:int=0:int
// test.kt:13 box: a$iv:int=1:int, $i$f$fold:int=0:int, before$iv:int=1:int
// test.kt:17 box: a$iv:int=1:int, $i$f$fold:int=0:int, before$iv:int=1:int, p0:int=1:int, $i$a$-fold-TestKt$box$1:int=0:int
// test.kt:7 box: a$iv:int=1:int, $i$f$fold:int=0:int, before$iv:int=1:int, p0:int=1:int, $i$a$-fold-TestKt$box$1:int=0:int, x$iv:int=1:int, $i$f$identity:int=0:int
// test.kt:8 box: a$iv:int=1:int, $i$f$fold:int=0:int, before$iv:int=1:int, p0:int=1:int, $i$a$-fold-TestKt$box$1:int=0:int, x$iv:int=1:int, $i$f$identity:int=0:int, y$iv:int=1:int
// test.kt:13 box: a$iv:int=1:int, $i$f$fold:int=0:int, before$iv:int=1:int
// test.kt:18 box:
