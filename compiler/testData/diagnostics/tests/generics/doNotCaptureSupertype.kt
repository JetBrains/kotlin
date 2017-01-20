// !CHECK_TYPE
// SKIP_TXT
fun test(b: S) {
    b.collect(toList()) checkType { _<Inv<String>>() }
}

interface S {
    fun <R> collect(collector: C<in String, R>): R
}

class C<X, Y>

fun <T> toList(): C<T, Inv<T>> = null!!

class Inv<Q>
