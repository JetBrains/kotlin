// !DIAGNOSTICS: -UNUSED_PARAMETER
@DslMarker
@Target(AnnotationTarget.TYPE)
annotation class L1

@DslMarker
@Target(AnnotationTarget.TYPE)
annotation class L2

class A {
    fun a() = 1
}

class B {
    fun b() = 2
}

fun foo1(x: (@L1 A).() -> Unit) {}
fun foo2(x: (@L2 A).() -> Unit) {}
fun bar1(x: (@L1 B).() -> Unit) {}
fun bar2(x: (@L2 B).() -> Unit) {}

fun test() {
    foo1 {
        a()

        foo2 {
            a()

            bar1 {
                a()
                b()
                bar2 {
                    <!DSL_SCOPE_VIOLATION!>a<!>()
                    b()
                }
            }

            bar2 {
                <!DSL_SCOPE_VIOLATION!>a<!>()
                b()
            }
        }

        bar1 {
            <!DSL_SCOPE_VIOLATION!>a<!>()
            b()
            bar2 {
                <!DSL_SCOPE_VIOLATION!>a<!>()
                b()
            }
        }

        bar2 {
            a()
            b()
        }
    }

    foo2 {
        a()

        bar1 {
            a()
            b()
            bar2 {
                <!DSL_SCOPE_VIOLATION!>a<!>()
                b()
            }
        }

        bar2 {
            <!DSL_SCOPE_VIOLATION!>a<!>()
            b()
        }
    }

    bar1 {
        b()
        bar2 {
            b()
        }
    }

    bar2 {
        b()
    }

    foo1 {
        bar1 {
            <!DSL_SCOPE_VIOLATION!>a<!>()
            b()
            foo2 {
                a()
                b()

                bar2 {
                    <!DSL_SCOPE_VIOLATION!>a<!>()
                    b()
                }
            }
        }
    }
}
