
interface A
open class B : A {
    companion object
}

fun <T> B.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<B, T>.() -> Unit = {}): B {
    TODO()
}

open class C : A {
    companion object
}

fun <T> C.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<C, T>.() -> Unit = {}): C {
    TODO()
}

fun <T: C> fVariableWithUpperBound(a: T) {}

fun main() {
    fVariableWithUpperBound([1, 2, 3])
}