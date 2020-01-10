// SKIP_TXT
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +NewInference

class A<X>

class B<T> {
    fun b(): T = TODO()
}

fun <Y> foo(c: A<Y>): Y = TODO()

fun <E> main(a: A<E>) {
    a as A<B<*>>

    foo(a).b()
}
