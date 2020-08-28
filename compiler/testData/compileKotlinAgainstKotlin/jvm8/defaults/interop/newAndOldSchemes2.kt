// FILE: 1.kt
// !JVM_DEFAULT_MODE: disable

interface Foo<T> {
    fun test(p: T) = "fail"
    val T.prop: String
        get() = "fail"
}

// FILE: main.kt
// !JVM_DEFAULT_MODE: all
// JVM_TARGET: 1.8
interface Foo2: Foo<String> {
    override fun test(p: String) = p

    override val String.prop: String
        get() = this
}

interface Foo3: Foo<String>, Foo2

class Base : Foo3

fun box(): String {
    val base = Base()
    return base.test("O") + with(base) { "K".prop }
}
