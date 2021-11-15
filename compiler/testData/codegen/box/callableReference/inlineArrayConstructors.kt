// WITH_STDLIB

fun createArray(ctor: (Int, (Int) -> Char) -> CharArray) =
    ctor(1) { 'O' }

inline fun createArrayInline(ctor: (Int, (Int) -> Char) -> CharArray) =
    ctor(1) { 'K' }

fun box(): String =
    createArray(::CharArray)[0].toString() + createArrayInline(::CharArray)[0].toString()
