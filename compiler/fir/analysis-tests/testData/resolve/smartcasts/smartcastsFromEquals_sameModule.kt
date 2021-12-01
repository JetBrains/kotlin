// ISSUE: KT-49127

class Final<T>

open class Base<T>

class Derived<T> : Base<T>()

class FinalWithOverride<T> {
    override fun equals(other: Any?): Boolean {
        // some custom implementation
        return this === other
    }
}

fun testFinal(x: Final<*>, y: Final<Int>) {
    if (x == y) {
        takeIntFinal(x) // OK
    }
    if (x === y) {
        takeIntFinal(x) // OK
    }
}

fun testBase(x: Base<*>, y: Base<Int>) {
    if (x == y) {
        takeIntBase(<!ARGUMENT_TYPE_MISMATCH!>x<!>) // Error
    }
    if (x === y) {
        takeIntBase(x) // OK
    }
}

fun testDerived(x: Derived<*>, y: Derived<Int>) {
    if (x == y) {
        takeIntDerived(x) // OK
    }
    if (x === y) {
        takeIntDerived(x) // OK
    }
}

fun testFinalWithOverride(x: FinalWithOverride<*>, y: FinalWithOverride<Int>) {
    if (x == y) {
        takeIntFinalWithOverride(<!ARGUMENT_TYPE_MISMATCH!>x<!>) // Error
    }
    if (x === y) {
        takeIntFinalWithOverride(x) // OK
    }
}

fun takeIntFinal(x: Final<Int>) {}
fun takeIntBase(x: Base<Int>) {}
fun takeIntDerived(x: Derived<Int>) {}
fun takeIntFinalWithOverride(x: FinalWithOverride<Int>) {}
