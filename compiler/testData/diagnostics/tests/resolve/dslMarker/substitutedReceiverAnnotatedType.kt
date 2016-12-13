// !DIAGNOSTICS: -UNUSED_PARAMETER
@DslMarker
@Target(AnnotationTarget.TYPE)
annotation class Ann

class A {
    fun a() = 1
}

class B {
    fun b() = 2
}

fun <T> foo(x: (@Ann T).() -> Unit) {}
fun <E> bar(x: (@Ann E).() -> Unit) {}

fun test() {
    foo<A> {
        a()
        bar<B> {
            <!DSL_SCOPE_VIOLATION!>a<!>()
            this@foo.a()
            b()
        }
    }
}
