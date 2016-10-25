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

fun test(a: A, b: B) {
    with(a) l1@{
        a()
        with(b) {
            <!DSL_SCOPE_VIOLATION!>a<!>()
            this@l1.a()
            b()
        }
    }
}
