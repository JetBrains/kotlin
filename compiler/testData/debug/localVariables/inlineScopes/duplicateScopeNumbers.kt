// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: test.kt
// KT-91569: scope numbers stay unique when an inlined body has gaps in its numbering,
// e.g. after the markers of `@InlineOnly` functions (`require`, `onFailure`) were dropped.

inline fun <T> wrap(block: () -> T): T = block()

inline fun get(x: Int): Int {
    val y = x + 1
    return y
}

inline fun withStop(block: () -> Unit) {
    runCatching(block).onFailure { e -> println(e) }
}

fun box() {
    val n = 1
    wrap {
        require(n > 0)
        val a = get(n)
        a
    }.also { println(it) }
    withStop { throw IllegalStateException("x") }
}

// EXPECTATIONS JVM +USE_INLINE_SCOPES_NUMBERS
// test.kt:19 box:
// test.kt:20 box: n:int=1:int
// test.kt:7 box: n:int=1:int, $i$f$wrap\1\20:int=0:int
// test.kt:21 box: n:int=1:int, $i$f$wrap\1\20:int=0:int, $i$a$-wrap-TestKt$box$1\2\75\0:int=0:int
// test.kt:22 box: n:int=1:int, $i$f$wrap\1\20:int=0:int, $i$a$-wrap-TestKt$box$1\2\75\0:int=0:int
// test.kt:10 box: n:int=1:int, $i$f$wrap\1\20:int=0:int, $i$a$-wrap-TestKt$box$1\2\75\0:int=0:int, x\3:int=1:int, $i$f$get\3\22:int=0:int
// test.kt:11 box: n:int=1:int, $i$f$wrap\1\20:int=0:int, $i$a$-wrap-TestKt$box$1\2\75\0:int=0:int, x\3:int=1:int, $i$f$get\3\22:int=0:int, y\3:int=2:int
// test.kt:22 box: n:int=1:int, $i$f$wrap\1\20:int=0:int, $i$a$-wrap-TestKt$box$1\2\75\0:int=0:int
// test.kt:23 box: n:int=1:int, $i$f$wrap\1\20:int=0:int, $i$a$-wrap-TestKt$box$1\2\75\0:int=0:int, a\2:int=2:int
// test.kt:7 box: n:int=1:int, $i$f$wrap\1\20:int=0:int
// test.kt:24 box: n:int=1:int
// fake.kt:1 box: n:int=1:int, it\4:int=2:int
// test.kt:24 box: n:int=1:int, it\4:int=2:int, $i$a$-also-TestKt$box$2\4\24\0:int=0:int
// test.kt:25 box: n:int=1:int
// test.kt:15 box: n:int=1:int, $i$f$withStop\5\25:int=0:int
// test.kt:25 box: n:int=1:int, $i$f$withStop\5\25:int=0:int, $i$a$-withStop-TestKt$box$3\7\78\0:int=0:int
// test.kt:15 box: n:int=1:int, $i$f$withStop\5\25:int=0:int
// fake.kt:1 box: n:int=1:int, $i$f$withStop\5\25:int=0:int, e\6:java.lang.Throwable=java.lang.IllegalStateException
// test.kt:15 box: n:int=1:int, $i$f$withStop\5\25:int=0:int, e\6:java.lang.Throwable=java.lang.IllegalStateException, $i$a$-onFailure-TestKt$withStop$1\6\78\5:int=0:int
// test.kt:16 box: n:int=1:int, $i$f$withStop\5\25:int=0:int
// test.kt:26 box: n:int=1:int

// EXPECTATIONS JVM
// test.kt:19 box:
// test.kt:20 box: n:int=1:int
// test.kt:7 box: n:int=1:int, $i$f$wrap:int=0:int
// test.kt:21 box: n:int=1:int, $i$f$wrap:int=0:int, $i$a$-wrap-TestKt$box$1:int=0:int
// test.kt:22 box: n:int=1:int, $i$f$wrap:int=0:int, $i$a$-wrap-TestKt$box$1:int=0:int
// test.kt:10 box: n:int=1:int, $i$f$wrap:int=0:int, $i$a$-wrap-TestKt$box$1:int=0:int, x$iv:int=1:int, $i$f$get:int=0:int
// test.kt:11 box: n:int=1:int, $i$f$wrap:int=0:int, $i$a$-wrap-TestKt$box$1:int=0:int, x$iv:int=1:int, $i$f$get:int=0:int, y$iv:int=2:int
// test.kt:22 box: n:int=1:int, $i$f$wrap:int=0:int, $i$a$-wrap-TestKt$box$1:int=0:int
// test.kt:23 box: n:int=1:int, $i$f$wrap:int=0:int, $i$a$-wrap-TestKt$box$1:int=0:int, a:int=2:int
// test.kt:7 box: n:int=1:int, $i$f$wrap:int=0:int
// test.kt:24 box: n:int=1:int
// fake.kt:1 box: n:int=1:int, it:int=2:int
// test.kt:24 box: n:int=1:int, it:int=2:int, $i$a$-also-TestKt$box$2:int=0:int
// test.kt:25 box: n:int=1:int
// test.kt:15 box: n:int=1:int, $i$f$withStop:int=0:int
// test.kt:25 box: n:int=1:int, $i$f$withStop:int=0:int, $i$a$-withStop-TestKt$box$3:int=0:int
// test.kt:15 box: n:int=1:int, $i$f$withStop:int=0:int
// fake.kt:1 box: n:int=1:int, $i$f$withStop:int=0:int, e$iv:java.lang.Throwable=java.lang.IllegalStateException
// test.kt:15 box: n:int=1:int, $i$f$withStop:int=0:int, e$iv:java.lang.Throwable=java.lang.IllegalStateException, $i$a$-onFailure-TestKt$withStop$1$iv:int=0:int
// test.kt:16 box: n:int=1:int, $i$f$withStop:int=0:int
// test.kt:26 box: n:int=1:int
