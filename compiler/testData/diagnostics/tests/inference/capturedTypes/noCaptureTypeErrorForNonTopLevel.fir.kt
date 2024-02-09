// !DIAGNOSTICS: -UNUSED_PARAMETER
class A<T>
class B<T>

fun <E> foo(b: B<in A<E>>) {}
fun <E> baz(b: B<out A<E>>) {}

// See KT-13950
fun bar(b: B<in A<out Number>>, bOut: B<out A<out Number>>, bOut2: B<out A<Number>>) {
    foo(b)
    foo<Number>(b)

    <!CANNOT_INFER_PARAMETER_TYPE!>baz<!>(<!ARGUMENT_TYPE_MISMATCH!>bOut<!>)
    baz<Number>(<!ARGUMENT_TYPE_MISMATCH!>bOut<!>)

    baz(bOut2)
    baz<Number>(bOut2)
}
