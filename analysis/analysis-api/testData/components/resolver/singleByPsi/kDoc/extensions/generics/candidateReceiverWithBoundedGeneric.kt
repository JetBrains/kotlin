interface Bound
class DerivedBound : Bound

open class Base<T>
class Derived<T : Bound> : Base<T>()

fun Base<DerivedBound>.someExtension() {}

/**
 * [Derived.someEx<caret>tension]
 */
fun foo() {
}