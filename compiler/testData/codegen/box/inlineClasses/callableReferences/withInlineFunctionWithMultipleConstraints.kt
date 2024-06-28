// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6

interface Foo {
    fun foo(): String
    fun bar(): String
}

value class Value(val x: Int) : Foo, Comparable<Value> {
    override fun foo() = "FOO $x"

    override fun bar() = "BAR $x"

    override fun compareTo(other: Value) = x.compareTo(other.x)
}

inline fun <T> foo(a: T, b: T): String where T: Foo, T: Comparable<T> {
    return if (a > b) a.foo() else b.foo()
}

inline fun <T> bar(a: T, b: T): String where T: Comparable<T>, T: Foo {
    return if (a > b) a.bar() else b.bar()
}

fun box(): String {
    if (foo(Value(1), Value(2)) != "FOO 2") return "Fail"
    if (bar(Value(2), Value(1)) != "BAR 2") return "Fail"
    return "OK"
}