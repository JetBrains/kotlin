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

fun foo(x: (@Ann A).() -> Unit) {}
fun bar(x: (@Ann B).() -> Unit) {}

fun test() {
    foo {
        with(this) l1@{
            a()
            bar {
                with(this) {
                    <!DSL_SCOPE_VIOLATION!>a<!>()
                    this@l1.a()
                    b()
                }
            }
        }
    }
}
