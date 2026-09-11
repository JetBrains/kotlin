// TARGET_BACKEND: JVM
// USE_INLINE_SCOPES_NUMBERS
// FILE: test.kt
// KT-89206: a type parameter whose upper bound is mapped to a JVM primitive is mapped to that primitive as well,
// while its nullable counterpart is mapped to the boxed type. The parameter of a `$default` stub is made nullable,
// so `f(J)J` gets `f$default(Ljava/lang/Long;ILjava/lang/Object;)J` and the stub cannot reuse its own parameter
// slot for the body of `f`. The inlined `f` therefore stores `value` in a slot of its own, but only the stub's
// own `value` is named in the local variable table, so a single `value` per inline scope stays visible here.

@Suppress("UNCHECKED_CAST")
inline fun <T : Long> f(value: T = 42L as T): Long {
    return value
}

fun box() {
    f<Long>()
    f(1L)
}

// EXPECTATIONS JVM_IR
// test.kt:16 box:
// test.kt:11 box:
// test.kt:12 box: value\1:long=42:long, $i$f$f\1\16:int=0:int
// test.kt:11 box: value\1:long=42:long
// test.kt:17 box:
// test.kt:12 box: value\2:long=1:long, $i$f$f\2\17:int=0:int
// test.kt:18 box:
