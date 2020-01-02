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

fun bar(x: (@Ann B).() -> Unit) {}

fun @Ann A.test() {
    bar {
        a()
        this@test.a()
        b()
    }
}
