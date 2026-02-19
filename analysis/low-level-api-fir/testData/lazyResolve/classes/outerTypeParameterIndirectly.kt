// ISSUE: KT-82085

open class X<T> {
    open inner class Y
}

class A : X<String>() {
    class D<U : Y> construc<caret>tor()
}
