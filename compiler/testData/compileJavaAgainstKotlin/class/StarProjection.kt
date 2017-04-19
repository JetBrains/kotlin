package test

open class K<out T: K<T>> {
    fun foo(k: K<*>) {}
    fun foo(): K<*> = null!!
}

class Sub: K<K<*>>()

fun bar(k: K<*>) {}