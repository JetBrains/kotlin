// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class S(val string: String)

abstract class Base(val x: S)

class Test(x: S) : Base(x)

fun box() = Test(S("OK")).x.string