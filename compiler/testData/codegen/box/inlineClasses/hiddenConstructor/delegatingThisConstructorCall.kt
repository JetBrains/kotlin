// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class S(val string: String)

class Test(val x: S, val y: S) {
    constructor(x: S) : this(x, S("K"))

    val test = x.string + y.string
}

fun box() = Test(S("O")).test