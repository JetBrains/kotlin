package i

fun foo<R, T: List<R>>(r: R, list: T) {}

fun test1(i: Int, collection: Collection<Int>) {
    foo(i, collection)
}