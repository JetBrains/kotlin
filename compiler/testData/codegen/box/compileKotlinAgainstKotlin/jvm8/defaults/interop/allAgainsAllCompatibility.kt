// WITH_STDLIB
// JVM_TARGET: 1.8

// MODULE: lib
// JVM_DEFAULT_MODE: all-compatibility
// FILE: 1.kt

interface Foo<T> {
    fun test(p: T) = "fail"
    val T.prop: String
        get() = "fail"
}

// MODULE: main(lib)
// JVM_DEFAULT_MODE: all
// FILE: main.kt
interface Foo2: Foo<String> {
    override fun test(p: String) : String = p

    override val String.prop: String
        get() = this
}

interface Foo3: Foo<String>, Foo2

class Base : Foo3

fun box(): String {
    val base = Base()
    return base.test("O") + with(base) { "K".prop }
}
