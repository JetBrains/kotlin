package test

interface A<T> {
    fun foo(x: T!!)

    val w: T!!
}

class B<X>(r: X!!)

fun <K> inside() {
    object : A<K> {
        override fun foo(x: K!!) {
        }

        override val w: K!!
            get() = TODO("")
    }
}

fun <F> bar1(x: F!!) {}
fun <F> bar2(x: F) = x!!

val <E> E.nn: E!! get() = this!!

class Outer {
    abstract class R1<T, F : T!!> : A<T!!>
    abstract class R2<T, F : T!!> : A<T>

    abstract class W<T> : A<T>
}

typealias Alias<R> = A<R!!>
