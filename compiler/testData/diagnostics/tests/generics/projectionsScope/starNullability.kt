// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
// See KT-9893
open class A

public interface I<T : A> {
    public fun foo(): T?
}

fun acceptA(a: A) {
}

fun main(i: I<*>) {
    i.foo() checkType { _<A?>() }
    acceptA(<!TYPE_MISMATCH!>i.foo()<!>) // i.foo() should be nullable but isn't
}
