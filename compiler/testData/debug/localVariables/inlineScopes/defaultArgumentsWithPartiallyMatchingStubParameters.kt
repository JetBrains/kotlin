// TARGET_BACKEND: JVM
// USE_INLINE_SCOPES_NUMBERS
// FILE: test.kt
// KT-89206: `value` is boxed in the stub and copied to a local; `other` reuses the stub's slot.

@Suppress("UNCHECKED_CAST")
inline fun <T : Long> f(value: T = 42L as T, other: String = "a"): String {
    return "$value$other"
}

inline fun g(): String {
    return f<Long>()
}

fun box() {
    g()
    f(1L, "b")
}

// EXPECTATIONS JVM_IR
// test.kt:16 box:
// test.kt:12 box: $i$f$g\1\16:int=0:int
// test.kt:7 box: $i$f$g\1\16:int=0:int
// test.kt:8 box: $i$f$g\1\16:int=0:int, value\2:long=42:long, other\2:java.lang.String="a":java.lang.String, $i$f$f\2\34:int=0:int
// test.kt:7 box: $i$f$g\1\16:int=0:int, value\2:long=42:long, other\2:java.lang.String="a":java.lang.String
// test.kt:12 box: $i$f$g\1\16:int=0:int
// test.kt:17 box:
// test.kt:8 box: value\3:long=1:long, other\3:java.lang.String="b":java.lang.String, $i$f$f\3\17:int=0:int
// test.kt:18 box:
