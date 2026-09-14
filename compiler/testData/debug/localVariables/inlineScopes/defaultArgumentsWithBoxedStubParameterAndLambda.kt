// TARGET_BACKEND: JVM
// USE_INLINE_SCOPES_NUMBERS
// FILE: test.kt
// KT-89206: the same layout mismatch as in defaultArgumentsWithBoxedStubParameter.kt, but the function also takes
// an inline lambda, so this also pins the scope and surrounding scope numbers of a lambda passed to a `$default`
// stub that inlines its implementation the general way.

@Suppress("UNCHECKED_CAST")
inline fun <T : Long> f(value: T = 42L as T, block: (Long) -> Long): Long {
    return block(value)
}

inline fun g(): Long {
    return f<Long> { it + 1 }
}

fun box() {
    g()
    f(1L) { it + 2 }
}

// EXPECTATIONS JVM_IR
// test.kt:18 box:
// test.kt:14 box: $i$f$g\1\18:int=0:int
// test.kt:9 box: $i$f$g\1\18:int=0:int
// test.kt:10 box: $i$f$g\1\18:int=0:int, value\2:long=42:long, $i$f$f\2\40:int=0:int
// test.kt:14 box: $i$f$g\1\18:int=0:int, value\2:long=42:long, $i$f$f\2\40:int=0:int, it\3:long=42:long, $i$a$-f$default-TestKt$g$1\3\42\1:int=0:int
// test.kt:10 box: $i$f$g\1\18:int=0:int, value\2:long=42:long, $i$f$f\2\40:int=0:int
// test.kt:9 box: $i$f$g\1\18:int=0:int, value\2:long=42:long
// test.kt:14 box: $i$f$g\1\18:int=0:int
// test.kt:19 box:
// test.kt:10 box: value\4:long=1:long, $i$f$f\4\19:int=0:int
// test.kt:19 box: value\4:long=1:long, $i$f$f\4\19:int=0:int, it\5:long=1:long, $i$a$-f-TestKt$box$1\5\43\0:int=0:int
// test.kt:10 box: value\4:long=1:long, $i$f$f\4\19:int=0:int
// test.kt:20 box:
