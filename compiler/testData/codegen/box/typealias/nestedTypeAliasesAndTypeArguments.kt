// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +NestedTypeAliases

// FILE: typealiases.kt

class Foo<T> {
    inner class Inner(val p: T) {
        inner class InnerInsideInner<K>(val p: K)
    }
    inner class Inner2<T2>(val p: T2)

    typealias TAtoInner = Foo<String>.Inner
    typealias TAtoInner2<S> = Foo<String>.Inner2<S>
    typealias TAtoInnerInsideInner<L> = Foo<String>.Inner.InnerInsideInner<L>
}

// FILE: main.kt

import Foo.TAtoInner
import Foo.TAtoInner2
import Foo.TAtoInnerInsideInner

fun box(): String {
    val foo = Foo<String>()
    val inner = foo.TAtoInner("OK")

    if (inner.p != "OK") return "FAIL"
    if (foo.TAtoInner2(42).p != 42) return "FAIL"
    if (inner.TAtoInnerInsideInner('c').p != 'c') return "FAIL"

    val callable = Foo<String>::TAtoInner
    if (callable(foo, "OK").p != "OK") return "FAIL"

    return "OK"
}