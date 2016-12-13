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

fun foo12(x: (@L1 @L2 A).() -> Unit) {}

fun bar1(x: (@L1 B).() -> Unit) {}
fun bar2(x: (@L2 B).() -> Unit) {}

fun <T> bar1t(q: T, x: (@L1 T).() -> Unit) {}

fun test() {
    foo12 {
        a()
        bar1 {
            <!DSL_SCOPE_VIOLATION!>a<!>()
            b()
        }

        bar2 {
            <!DSL_SCOPE_VIOLATION!>a<!>()
            b()
        }
    }

    bar1 {
        b()
        foo12 {
            a()
            <!DSL_SCOPE_VIOLATION!>b<!>()
        }
    }

    bar2 {
        b()
        foo12 {
            a()
            <!DSL_SCOPE_VIOLATION!>b<!>()
        }
    }

    foo2 {
        bar1t(this) {
            a()
            bar1 {
                <!DSL_SCOPE_VIOLATION!>a<!>()
                b()
            }

            bar2 {
                <!DSL_SCOPE_VIOLATION!>a<!>()
                b()
            }
        }
    }

    bar1 {
        b()
        foo2 {
            bar1t(this) {
                a()
                <!DSL_SCOPE_VIOLATION!>b<!>()
            }
        }
    }

    bar2 {
        b()
        foo2 {
            bar1t(this) {
                a()
                <!DSL_SCOPE_VIOLATION!>b<!>()
            }
        }
    }
}
