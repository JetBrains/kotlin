// NAME: B

// INFO: {checked: "true"}
interface I<T>

open class J<T>

// SIBLING:
class <caret>A<T, U : List<T>, V, W, X> : J<X>(), I<W> {
    // INFO: {checked: "true", toAbstract: "true"}
    fun foo() {
        val u: U
    }
}