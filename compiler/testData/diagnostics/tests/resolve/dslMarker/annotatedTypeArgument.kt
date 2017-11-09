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

fun <T> foo(x: T.() -> Unit) {}
fun <E> bar(x: E.() -> Unit) {}

fun test() {
    foo<@Ann A> {
        a()
        bar<@Ann B> {
            <!DSL_SCOPE_VIOLATION!>a<!>()
            this@foo.a()
            b()
        }
    }
}
