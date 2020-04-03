// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-33263

class A
class Foo<U>
class Bar<R>

fun <T> Foo<in T>.create(): Bar<in T> = null!!//Bar()
//fun <T> Foo<in T>.create(): Bar<T> = null!!//Bar()
fun <K> convert(bar: Bar<in K>): Bar<K> = null!!//Bar()

fun test(x: Any) {}
fun take(x: Bar<A>) {}

fun test_1(foo: Foo<A>) {
//    convert(foo.create())
    convert(foo.create())
}

fun test_2(foo: Foo<A>) {
    test(convert(foo.create()))
}
