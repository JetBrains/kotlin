// IGNORE_BACKEND_FIR: JVM_IR
// WITH_STDLIB
// JVM_TARGET: 1.8
// MODULE: lib
// !JVM_DEFAULT_MODE: disable
// FILE: 1.kt

interface Foo<T> {
    fun test(p: T) = p
    val T.prop: String
        get() = "K"
}

interface FooDerived: Foo<String>

// MODULE: main(lib)
// !JVM_DEFAULT_MODE: all-compatibility
// FILE: main.kt
open class UnspecializedFromDerived : FooDerived

fun box(): String {
    val foo = UnspecializedFromDerived()
    return foo.test("O") + with(foo) { "K".prop }
}
