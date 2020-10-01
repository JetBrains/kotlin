// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// JVM_TARGET: 1.8
// FILE: 1.kt
// !JVM_DEFAULT_MODE: disable

interface Foo<T> {
    fun test(p: T) = p
    val T.prop: String
        get() = "K"
}

interface FooDerived: Foo<String>

// FILE: main.kt
// !JVM_DEFAULT_MODE: all-compatibility
open class UnspecializedFromDerived : FooDerived

fun box(): String {
    val foo = UnspecializedFromDerived()
    return foo.test("O") + with(foo) { "K".prop }
}
