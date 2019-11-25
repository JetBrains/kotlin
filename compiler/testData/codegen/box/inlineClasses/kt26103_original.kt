// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

inline class Foo<T>(val x: Int)

class Bar(val y: Foo<Any>)

fun box(): String {
    if (Bar(Foo<Any>(42)).y.x != 42) throw AssertionError()

    return "OK"
}