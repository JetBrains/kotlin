// WITH_STDLIB

// KT-59544

inline fun h(b: (Int) -> Array<String?>) = b(1).size

fun box(): String = ('N' + h(::arrayOfNulls)) + "K"