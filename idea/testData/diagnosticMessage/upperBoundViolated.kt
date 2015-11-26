// !DIAGNOSTICS_NUMBER: 3
// !DIAGNOSTICS: TYPE_INFERENCE_UPPER_BOUND_VIOLATED
// !MESSAGE_TYPE: TEXT

package i

fun <R, T: List<R>> foo(r: R, list: T) {}

fun test1(i: Int, collection: Collection<Int>) {
    foo(i, collection) //error
}

//--------------
fun <V : U, U> bar(v: V, u: MutableSet<U>) = u

fun test2(a: Any, s: MutableSet<String>) {
    bar(a, s) //error
}

//--------------
interface A
class B

fun <T: R, R: B> baz(t: T, r: R) where T: A {

}

fun test3(a: A, b: B) {
    baz(a, b) //error
}
