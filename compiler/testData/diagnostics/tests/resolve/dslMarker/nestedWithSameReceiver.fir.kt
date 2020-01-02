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
                a()
                this@l1.a()
                b()

                foo l3@{
                    a()
                    b()
                    this@l2.b()
                    bar {
                        a()
                        this@l3.a()
                        b()
                    }
                }
            }
        }
    }
}
