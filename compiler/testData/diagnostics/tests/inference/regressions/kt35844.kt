// SKIP_TXT
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +NewInference

class A<X>

class B<T> {
    fun b(): T = TODO()
}

fun <Y> foo(c: A<Y>): Y = TODO()

fun <E> main(a: A<E>) {
    a <!UNCHECKED_CAST!>as A<B<*>><!>

    foo(<!DEBUG_INFO_SMARTCAST!>a<!>).b()
}
