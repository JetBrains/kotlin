class A<E> {
    fun foo(): E = TODO()
}

class B(var a: A<*>?) {
    fun bar() {
        if (a != null) {
            a.<!INAPPLICABLE_CANDIDATE!>foo<!>()
        }
    }
}
