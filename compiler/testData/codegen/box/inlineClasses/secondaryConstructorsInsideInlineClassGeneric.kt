// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

@file:Suppress("SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_VALUE_CLASS")

var global = "wrong"

OPTIONAL_JVM_INLINE_ANNOTATION
value class Foo<T: String>(val x: T) {
    constructor(y: Int) : this(y.toString() as T)
    constructor(z: Long) : this(z.toInt() + 1)
    constructor(other: Char) : this(other.toInt().toString() as T) {
        global = "OK"
    }
    constructor(a: Int, b: Int) : this((a + b).toString() as T)
}

fun box(): String {
    var f = Foo<String>(42)
    if (f.x != "42") return "Fail 1: ${f.x}"

    f = Foo<String>(43L)
    if (f.x != "44") return "Fail 2: ${f.x}"

    f = Foo<String>('a')
    if (f.x != "97") return "Fail 3: ${f.x}"

    f = Foo<String>(1, 2)
    if (f.x != "3") return "Fail 4: ${f.x}"

    return global
}