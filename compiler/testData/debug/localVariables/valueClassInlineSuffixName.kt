// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: test.kt

@JvmInline
value class IntHolder(val value: Int)

// The encoded name of a variable `iv` ends with `$iv`, like the suffix of an inlined variable
inline fun outer(): Int {
    val iv: UInt = 7u
    return inner(IntHolder(iv.toInt()))
}

inline fun inner(iv: IntHolder): Int {
    return iv.value
}

fun box() {
    outer()
}

// EXPECTATIONS JVM +USE_INLINE_SCOPES_NUMBERS
// test.kt:19 box:
// test.kt:10 box: $i$f$outer\1\19:int=0:int
// test.kt:11 box: $i$f$outer\1\19:int=0:int, $v$c$kotlin$_UInt$$iv\1:int=7:int
// test.kt:15 box: $i$f$outer\1\19:int=0:int, $v$c$kotlin$_UInt$$iv\1:int=7:int, $v$c$IntHolder$$iv\2:int=7:int, $i$f$inner-GcG_7hs\2\40:int=0:int
// test.kt:11 box: $i$f$outer\1\19:int=0:int, $v$c$kotlin$_UInt$$iv\1:int=7:int
// test.kt:20 box:

// EXPECTATIONS JVM
// test.kt:19 box:
// test.kt:10 box: $i$f$outer:int=0:int
// test.kt:11 box: $i$f$outer:int=0:int, $v$c$kotlin$_UInt$$iv$iv:int=7:int
// test.kt:15 box: $i$f$outer:int=0:int, $v$c$kotlin$_UInt$$iv$iv:int=7:int, $v$c$IntHolder$$iv$iv$iv:int=7:int, $i$f$inner-GcG_7hs:int=0:int
// test.kt:11 box: $i$f$outer:int=0:int, $v$c$kotlin$_UInt$$iv$iv:int=7:int
// test.kt:20 box:
