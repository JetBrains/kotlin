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

fun bar(x: B.() -> Unit) {}

fun A.test() {
    bar {
        <!DSL_SCOPE_VIOLATION!>a<!>()
        this@test.a()
        b()
    }
}
