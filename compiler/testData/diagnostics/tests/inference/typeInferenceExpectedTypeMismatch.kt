// !WITH_NEW_INFERENCE
package typeInferenceExpectedTypeMismatch

import java.util.*

fun test() {
    val s : Set<Int> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH{OI}, TYPE_MISMATCH{NI}!>newList()<!>
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

    val b: Two<A, C> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH{OI}, TYPE_MISMATCH{NI}!>foo(outA, inB)<!>
    use(b)
}

fun <T> bar(o: Out<T>, i: In<T>): Two<T, T> = throw Exception("$o $i")

fun test2(outA: Out<A>, inC: In<C>) {
    <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS{OI}!>bar<!>(outA, <!TYPE_MISMATCH{NI}!>inC<!>)

    val b: Two<A, B> = <!TYPE_MISMATCH{NI}, TYPE_MISMATCH{NI}, TYPE_MISMATCH{NI}, TYPE_MISMATCH{NI}, TYPE_MISMATCH{NI}!><!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS{OI}!>bar<!>(outA, <!TYPE_MISMATCH{NI}!>inC<!>)<!>
    use(b)
}

fun use(vararg a: Any?) = a
