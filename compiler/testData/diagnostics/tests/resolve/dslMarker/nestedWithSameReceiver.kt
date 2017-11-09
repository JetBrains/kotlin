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
        a()
        foo l1@{
            a()
            bar l2@{
                <!DSL_SCOPE_VIOLATION!>a<!>()
                this@l1.a()
                b()

                foo l3@{
                    a()
                    <!DSL_SCOPE_VIOLATION!>b<!>()
                    this@l2.b()
                    bar {
                        <!DSL_SCOPE_VIOLATION!>a<!>()
                        this@l3.a()
                        b()
                    }
                }
            }
        }
    }
}
