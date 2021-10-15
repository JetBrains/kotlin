
interface A
open class B : A {
    companion object
}

fun <K, V> B.Companion.buildDict(size: Int, init: DictCollectionLiteralBuilder<B, K, V>.() -> Unit = {}): B {
    TODO()
}

open class C : A {
    companion object
}

fun <K, V> C.Companion.buildDict(size: Int, init: DictCollectionLiteralBuilder<C, K, V>.() -> Unit = {}): C {
    TODO()
}

fun <T: C> fVariableWithUpperBound(a: T) {}

fun main() {
    fVariableWithUpperBound(["1": 1, "2": 2, "3": 3])
}