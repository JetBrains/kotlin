// !CHECK_TYPE

class A<T> {
    fun foo(): T = null!!
}

fun <E> A<E>.bar(): A<in E> = this

fun baz(x: A<out CharSequence>) {
    x.bar() checkType { _<A<*>>() }
    x.bar().foo() checkType { _<Any?>() } // See KT-10448
}
