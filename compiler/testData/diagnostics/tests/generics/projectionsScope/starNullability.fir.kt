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
    i.foo() checkType { <!UNRESOLVED_REFERENCE!>_<!><A?>() }
    acceptA(i.foo()) // i.foo() should be nullable but isn't
}
