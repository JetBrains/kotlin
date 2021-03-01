// !WITH_NEW_INFERENCE
package typeInferenceExpectedTypeMismatch

import java.util.*

fun test() {
    val s : Set<Int> = <!INITIALIZER_TYPE_MISMATCH!>newList()<!>
    use(s)
}

fun <S> newList() : ArrayList<S> {
    return ArrayList<S>()
}

interface Out<out T>
interface In<in T>

interface Two<T, R>

interface A
interface B: A
interface C: A

fun <T, R> foo(o: Out<T>, i: In<R>): Two<T, R> = throw Exception("$o $i")

fun test1(outA: Out<A>, inB: In<B>) {
    foo(outA, inB)

    val b: Two<A, C> = foo(outA, inB)
    use(b)
}

fun <T> bar(o: Out<T>, i: In<T>): Two<T, T> = throw Exception("$o $i")

fun test2(outA: Out<A>, inC: In<C>) {
    <!INAPPLICABLE_CANDIDATE!>bar<!>(outA, inC)

    val b: Two<A, B> = <!INITIALIZER_TYPE_MISMATCH!><!INAPPLICABLE_CANDIDATE!>bar<!>(outA, inC)<!>
    use(b)
}

fun use(vararg a: Any?) = a
