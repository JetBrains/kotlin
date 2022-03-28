// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

abstract class Bar<T>

class Foo<T> : Bar<T>(), Comparable<Foo<*>> {
    override fun compareTo(other: Foo<*>): Int = TODO()
}

infix fun <T : Comparable<T>, S : T?> Bar<in S>.test(t: T) { }
infix fun <T : Comparable<T>, S : T?> Bar<in S>.test(other: Bar<in S>) {}

fun checkFunctions(exp1: Foo<Int?>, exp2: Foo<Int>) {
    exp1.test(exp2)
}