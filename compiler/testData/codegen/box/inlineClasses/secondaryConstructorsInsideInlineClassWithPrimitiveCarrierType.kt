// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR, JS_IR

var global = "wrong"

inline class Foo(val x: Int) {
    constructor(y: String) : this(y.length)

    constructor(z: Long) : this(z.toInt() + 1)

    @Suppress("SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_INLINE_CLASS")
    constructor(other: Char) : this(other.toInt().toString()) {
        global = "OK"
    }

    constructor(a: Int, b: Int) : this(a + b)
}

fun box(): String {
    var f = Foo("42")
    if (f.x != 2) return "Fail 1: ${f.x}"

    f = Foo(43L)
    if (f.x != 44) return "Fail 2: ${f.x}"

    f = Foo('a')
    if (f.x != 2) return "Fail 3: ${f.x}"

    f = Foo(1, 2)
    if (f.x != 3) return "Fail 4: ${f.x}"

    return global
}