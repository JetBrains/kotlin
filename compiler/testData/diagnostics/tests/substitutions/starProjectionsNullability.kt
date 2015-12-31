open class A

public interface I<T : A> {
    public fun foo(): T?
}

fun acceptA(<!UNUSED_PARAMETER!>a<!>: A) {
}

fun checkA(a: I<*>) {
    acceptA(<!TYPE_MISMATCH!>a.foo()<!>)
}
