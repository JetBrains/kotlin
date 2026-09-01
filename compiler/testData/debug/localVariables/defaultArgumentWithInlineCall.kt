// TARGET_BACKEND: JVM
// FILE: test.kt

inline fun id(x: Int): Int = x

inline fun f(
    x: Int = id(2),
    y: Int = 3
): Int {
    return x * y
}

inline fun g() {
    f()
}

fun box() {
    g()
}

// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:18 box:
// test.kt:14 box: $i$f$g\1\18:int=0:int
// test.kt:7 box: $i$f$g\1\18:int=0:int
// test.kt:4 box: $i$f$g\1\18:int=0:int, x\2:int=2:int, $i$f$id\2\53:int=0:int
// test.kt:8 box: $i$f$g\1\18:int=0:int, x\3:int=2:int
// test.kt:6 box: $i$f$g\1\18:int=0:int, x\3:int=2:int, y\3:int=3:int
// test.kt:10 box: $i$f$g\1\18:int=0:int, x\3:int=2:int, y\3:int=3:int, $i$f$f\3\-1:int=0:int
// test.kt:15 box: $i$f$g\1\18:int=0:int
// test.kt:19 box:

// EXPECTATIONS JVM_IR
// test.kt:18 box:
// test.kt:14 box: $i$f$g:int=0:int
// test.kt:7 box: $i$f$g:int=0:int
// test.kt:4 box: $i$f$g:int=0:int, x$iv$iv$iv:int=2:int, $i$f$id:int=0:int
// test.kt:8 box: $i$f$g:int=0:int, x$iv$iv:int=2:int
// test.kt:6 box: $i$f$g:int=0:int, x$iv$iv:int=2:int, y$iv$iv:int=3:int
// test.kt:10 box: $i$f$g:int=0:int, x$iv$iv:int=2:int, y$iv$iv:int=3:int, $i$f$f:int=0:int
// test.kt:15 box: $i$f$g:int=0:int
// test.kt:19 box:
