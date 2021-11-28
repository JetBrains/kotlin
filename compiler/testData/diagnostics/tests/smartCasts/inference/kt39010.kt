// FIR_IDENTICAL
class A<E> {
    fun foo(): E = TODO()
}

class B(var a: A<*>?) {
    fun bar() {
        if (a != null) {
            <!SMARTCAST_IMPOSSIBLE!>a<!>.foo()
        }
    }
}
