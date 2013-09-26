package i

fun foo<R, T: List<R>>(r: R, list: T) {}

fun test1(i: Int, collection: Collection<Int>) {
    foo(i, collection) //error
}

//--------------
fun bar<V : U, U>(v: V, u: MutableSet<U>) = u

fun test2(a: Any, s: MutableSet<String>) {
    bar(a, s) //error
}

//--------------
trait A
class B

fun baz<T: R, R: B>(t: T, r: R) where T: A {

}

fun test3(a: A, b: B) {
    baz(a, b) //error
}
