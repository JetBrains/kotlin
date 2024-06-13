// SKIP_TXT
// DIAGNOSTICS: -UNUSED_PARAMETER

class A<X>

class B<T> {
    fun b(): T = TODO()
}

fun <Y> foo(c: A<Y>): Y = TODO()

fun <E> main(a: A<E>) {
    a <!UNCHECKED_CAST!>as A<B<*>><!>

    foo(a).b()
}

class AOut<out X>

fun <Y> foo(c: AOut<Y>): Y = TODO()

fun <E> mainOut(a: AOut<E>) {
    a <!UNCHECKED_CAST!>as AOut<B<*>><!>

    foo(a).b()
}
