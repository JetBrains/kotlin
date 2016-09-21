// !DIAGNOSTICS: -UNUSED_PARAMETER
class A<T>
class B<T>

fun <E> foo(b: B<in A<E>>) {}
fun <E> baz(b: B<out A<E>>) {}

// See KT-13950
fun bar(b: B<in A<out Number>>, bOut: B<out A<out Number>>, bOut2: B<out A<Number>>) {
    foo(b)
    foo<Number>(b)

    <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>baz<!>(bOut)
    baz<Number>(<!TYPE_MISMATCH!>bOut<!>)

    baz(bOut2)
    baz<Number>(bOut2)
}
