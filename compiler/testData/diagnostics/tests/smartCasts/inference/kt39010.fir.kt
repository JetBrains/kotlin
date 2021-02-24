class A<E> {
    fun foo(): E = TODO()
}

class B(var a: A<*>?) {
    fun bar() {
        if (a != null) {
            a<!UNSAFE_CALL!>.<!>foo()
        }
    }
}
