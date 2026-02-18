// WITH_STDLIB
// FILE: lib.kt

inline fun createArrayInline(ctor: (Int, (Int) -> Char) -> CharArray) =
    ctor(1) { 'K' }

// FILE: main.kt
fun createArray(ctor: (Int, (Int) -> Char) -> CharArray) =
    ctor(1) { 'O' }

fun box(): String =
    createArray(::CharArray)[0].toString() + createArrayInline(::CharArray)[0].toString()
