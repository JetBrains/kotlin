// WITH_STDLIB

// KT-59544
// FILE: lib.kt

inline fun h(b: (Int) -> Array<String?>) = b(1).size
// FILE: main.kt

fun box(): String = ('N' + h(::arrayOfNulls)) + "K"