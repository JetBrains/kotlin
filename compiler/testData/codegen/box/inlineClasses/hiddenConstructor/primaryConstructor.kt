// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class S(val string: String)

class Test(val s: S)

fun box() = Test(S("OK")).s.string