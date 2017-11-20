// !WITH_NEW_INFERENCE
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
    operator fun B.invoke() {}

    val B.y: D get() = D()
}

class B

class C {
    operator fun D.invoke() {}

    val D.x: B get() = B()
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
                D().x()

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
                    D().<!DSL_SCOPE_VIOLATION!>x<!>()
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
