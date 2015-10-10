// !DIAGNOSTICS: -UNUSED_PARAMETER
//KT-6081 Chained generic method calls: wrong type inference

class Bar<T>

fun <T> bar(): Bar<T> = null!!

class Foo {
    fun <R> add(bar: Bar<R>): Foo {return this}
}

fun doesNotWork(bi: Bar<Int>, bs: Bar<String>) {
    Foo().add(bi).add(bs)
}