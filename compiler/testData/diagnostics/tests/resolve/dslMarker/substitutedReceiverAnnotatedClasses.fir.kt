// !DIAGNOSTICS: -UNUSED_PARAMETER
@DslMarker
annotation class Ann

@Ann
class A {
    fun a() = 1
}

@Ann
class B {
    fun b() = 2
}

fun <T> foo(x: T.() -> Unit) {}
fun <E> bar(x: E.() -> Unit) {}

fun test() {
    foo<A> {
        a()
        bar<B> {
            a()
            this@foo.a()
            b()
        }
    }
}
