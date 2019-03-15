// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

@file:Suppress("SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_INLINE_CLASS")

var global = "wrong"

inline class Foo(val x: String) {
    constructor(y: Int) : this(y.toString())
    constructor(z: Long) : this(z.toInt() + 1)
    constructor(other: Char) : this(other.toInt().toString()) {
        global = "OK"
    }
    constructor(a: Int, b: Int) : this((a + b).toString())
}

fun box(): String {
    var f = Foo(42)
    if (f.x != "42") return "Fail 1: ${f.x}"

    f = Foo(43L)
    if (f.x != "44") return "Fail 2: ${f.x}"

    f = Foo('a')
    if (f.x != "97") return "Fail 3: ${f.x}"

    f = Foo(1, 2)
    if (f.x != "3") return "Fail 4: ${f.x}"

    return global
}