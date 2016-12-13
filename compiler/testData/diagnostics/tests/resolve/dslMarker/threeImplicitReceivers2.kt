// !DIAGNOSTICS: -UNUSED_PARAMETER
@DslMarker
@Target(AnnotationTarget.TYPE)
annotation class Ann1

@DslMarker
@Target(AnnotationTarget.TYPE)
annotation class Ann2

@DslMarker
@Target(AnnotationTarget.TYPE)
annotation class Ann3

class A {
    val B.y: (C.() -> Unit) get() = null!!
}

class B

class C {
    val D.x: (A.() -> Unit) get() = null!!
}

class D

fun foo(x: (@Ann1 A).() -> Unit) {}
fun bar(x: (@Ann2 B).() -> Unit) {}
fun baz(x: (@Ann3 C).() -> Unit) {}
fun foo1(x: (@Ann1 D).() -> Unit) {}
fun foo2(x: (@Ann2 D).() -> Unit) {}
fun foo3(x: (@Ann3 D).() -> Unit) {}

fun test() {
    foo {
        bar {
            baz {
                y()

                <!UNRESOLVED_REFERENCE_WRONG_RECEIVER, FUNCTION_EXPECTED!>x<!>()

                with(D()) {
                    x()
                }

                foo1 {
                    <!DSL_SCOPE_VIOLATION!>x<!>()
                    <!DSL_SCOPE_VIOLATION!>y<!>()

                    with(A()) {
                        x()
                        y()
                    }

                    with(D()) {
                        <!DSL_SCOPE_VIOLATION!>x<!>()
                    }
                    A().x()
                }

                foo2 {
                    x()
                    <!DSL_SCOPE_VIOLATION!>y<!>()
                }

                foo3 {
                    <!DSL_SCOPE_VIOLATION!>x<!>()
                    <!DSL_SCOPE_VIOLATION!>y<!>()
                }
            }
        }
    }

    foo1 {
        foo {
            baz {
                bar {
                    <!DSL_SCOPE_VIOLATION!>x<!>()
                    y()
                }
            }
        }
    }

    foo2 {
        foo {
            baz {
                bar {
                    <!DSL_SCOPE_VIOLATION!>x<!>()
                    y()
                }
            }
        }
    }

    foo3 {
        foo {
            baz {
                bar {
                    <!DSL_SCOPE_VIOLATION!>x<!>()
                    y()
                }
            }
        }
    }
}
