// !LANGUAGE: +InlineClasses

inline class S(val string: String)

sealed class Sealed(val x: S)

class Test(x: S) : Sealed(x)

fun box() = Test(S("OK")).x.string