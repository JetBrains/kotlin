// !LANGUAGE: +InlineClasses

inline class S(val string: String)

class Test(val s: S)

fun box() = Test(S("OK")).s.string