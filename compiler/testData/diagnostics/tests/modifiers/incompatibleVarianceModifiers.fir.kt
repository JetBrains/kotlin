// !DIAGNOSTICS: -UNUSED_VARIABLE

interface Foo<T>
interface Foo1<in out T>
interface Foo2<in in T>

fun test1(foo: Foo<in out Int>) = foo
fun test2(): Foo<in in Int> = throw Exception()

fun test3() {
    val f: Foo<out out out out Int>

    class Bzz<in in T>
}

class A {
    fun <out out T> bar() {
    }
}

fun test4(a: A) {
    a.bar<out out Int>()
}
