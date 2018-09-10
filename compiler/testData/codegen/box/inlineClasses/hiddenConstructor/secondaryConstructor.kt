// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class S(val string: String)

class Test(val s: S) {
    constructor(x: String, s: S) : this(S(x + s.string))
}

fun box() = Test("O", S("K")).s.string