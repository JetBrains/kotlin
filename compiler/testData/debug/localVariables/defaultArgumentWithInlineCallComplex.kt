// TARGET_BACKEND: JVM
// FILE: test.kt

inline fun id(x: Int): Int = x

inline fun applyIt(x: Int, block: (Int) -> Int): Int = block(x)

inline fun f(
    x: Int = applyIt(2) { it + 1 },
    y: Int = 3
): Int {
    return id(x) * y
}

inline fun g() {
    f()
}

fun box() {
    g()
}

// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:20 box:
// test.kt:16 box: $i$f$g\1\20:int=0:int
// test.kt:9 box: $i$f$g\1\20:int=0:int
// test.kt:6 box: $i$f$g\1\20:int=0:int, x\2:int=2:int, $i$f$applyIt\2\66:int=0:int
// test.kt:9 box: $i$f$g\1\20:int=0:int, x\2:int=2:int, $i$f$applyIt\2\66:int=0:int, it\3:int=2:int, $i$a$-applyIt-TestKt$f$1\3\69\2:int=0:int
// test.kt:6 box: $i$f$g\1\20:int=0:int, x\2:int=2:int, $i$f$applyIt\2\66:int=0:int
// test.kt:10 box: $i$f$g\1\20:int=0:int, x\4:int=3:int
// test.kt:8 box: $i$f$g\1\20:int=0:int, x\4:int=3:int, y\4:int=3:int
// test.kt:12 box: $i$f$g\1\20:int=0:int, x\4:int=3:int, y\4:int=3:int, $i$f$f\4\-1:int=0:int
// test.kt:4 box: $i$f$g\1\20:int=0:int, x\4:int=3:int, y\4:int=3:int, $i$f$f\4\-1:int=0:int, x\5:int=3:int, $i$f$id\5\75:int=0:int
// test.kt:12 box: $i$f$g\1\20:int=0:int, x\4:int=3:int, y\4:int=3:int, $i$f$f\4\-1:int=0:int
// test.kt:17 box: $i$f$g\1\20:int=0:int
// test.kt:21 box:

// EXPECTATIONS JVM_IR
// test.kt:20 box:
// test.kt:16 box: $i$f$g:int=0:int
// test.kt:9 box: $i$f$g:int=0:int
// test.kt:6 box: $i$f$g:int=0:int, x$iv$iv$iv:int=2:int, $i$f$applyIt:int=0:int
// test.kt:9 box: $i$f$g:int=0:int, x$iv$iv$iv:int=2:int, $i$f$applyIt:int=0:int, it$iv$iv:int=2:int, $i$a$-applyIt-TestKt$f$1$iv$iv:int=0:int
// test.kt:6 box: $i$f$g:int=0:int, x$iv$iv$iv:int=2:int, $i$f$applyIt:int=0:int
// test.kt:10 box: $i$f$g:int=0:int, x$iv$iv:int=3:int
// test.kt:8 box: $i$f$g:int=0:int, x$iv$iv:int=3:int, y$iv$iv:int=3:int
// test.kt:12 box: $i$f$g:int=0:int, x$iv$iv:int=3:int, y$iv$iv:int=3:int, $i$f$f:int=0:int
// test.kt:4 box: $i$f$g:int=0:int, x$iv$iv:int=3:int, y$iv$iv:int=3:int, $i$f$f:int=0:int, x$iv$iv$iv:int=3:int, $i$f$id:int=0:int
// test.kt:12 box: $i$f$g:int=0:int, x$iv$iv:int=3:int, y$iv$iv:int=3:int, $i$f$f:int=0:int
// test.kt:17 box: $i$f$g:int=0:int
// test.kt:21 box:
