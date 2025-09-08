interface Bound
class DerivedBound : Bound

open class Base<T>
class Derived : Base<DerivedBound>()

class DerivedWithIterable : Base<Iterable<DerivedBound>>()

fun <T: Bound> Base<T>.someExtension() {}

// Extra type parameter not part of the receiver.
fun <T : Bound, U : Bound> Base<T>.someExtension2(u: U) {}

// Extra type parameter not part of the receiver that uses a receiver type parameter.
fun <T : Bound, U : Iterable<T>> Base<T>.someExtension3(u: U) {}

// Type parameter of the receiver uses another type parameter not part of the receiver.
fun <T : Iterable<U>, U : Bound> Base<T>.someExtension4(u: U) {}

/**
 * [Derived.someE<caret_1>xtension]
 * [Derived.someE<caret_2>xtension2]
 * [Derived.someE<caret_3>xtension3]
 * [Derived.someE<caret_4>xtension4]
 * [DerivedWithIterable.someE<caret_5>xtension4]
 */
fun foo() {
}
