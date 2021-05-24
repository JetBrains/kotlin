// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER


interface Bar<T> {
    val t: T
}

class MyBar<T>(override val t: T) : Bar<T>

class BarR : Bar<BarR> {
    override val t: BarR get() = this
}

class Foo<F : Bar<F>>(val f: F)

fun <T> id(t1: T, t2: T) = t2

fun test(foo: Foo<*>, g: Bar<*>) {
    id(foo.f, g).t.<!UNRESOLVED_REFERENCE!>t<!>
}

fun main() {
    val foo = Foo(BarR())
    test(foo, MyBar(2))
}
