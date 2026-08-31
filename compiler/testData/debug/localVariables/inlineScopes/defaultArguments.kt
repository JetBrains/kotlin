// TARGET_BACKEND: JVM
// USE_INLINE_SCOPES_NUMBERS
// FILE: test.kt
// KT-86376: the marker of `f` must carry the call site line of `f()` in `g`, not the
// declaration line of `f`. The call site 37 is the fake SMAP line for line 15.

inline fun f(
    x: Int = 2,
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

// EXPECTATIONS JVM_IR
// test.kt:19 box:
// test.kt:15 box: $i$f$g\1\19:int=0:int
// test.kt:8 box: $i$f$g\1\19:int=0:int
// test.kt:9 box: $i$f$g\1\19:int=0:int, x\2:int=2:int
// test.kt:7 box: $i$f$g\1\19:int=0:int, x\2:int=2:int, y\2:int=3:int
// test.kt:11 box: $i$f$g\1\19:int=0:int, x\2:int=2:int, y\2:int=3:int, $i$f$f\2\37:int=0:int
// test.kt:16 box: $i$f$g\1\19:int=0:int
// test.kt:20 box:
