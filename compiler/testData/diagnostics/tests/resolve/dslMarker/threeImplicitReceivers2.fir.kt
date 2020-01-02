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

                <!UNRESOLVED_REFERENCE!>x<!>()

                with(D()) {
                    x()
                }

                foo1 {
                    x()
                    y()

                    with(A()) {
                        x()
                        y()
                    }

                    with(D()) {
                        x()
                    }
                    A().<!UNRESOLVED_REFERENCE!>x<!>()
                }

                foo2 {
                    x()
                    y()
                }

                foo3 {
                    x()
                    y()
                }
            }
        }
    }

    foo1 {
        foo {
            baz {
                bar {
                    x()
                    y()
                }
            }
        }
    }

    foo2 {
        foo {
            baz {
                bar {
                    x()
                    y()
                }
            }
        }
    }

    foo3 {
        foo {
            baz {
                bar {
                    x()
                    y()
                }
            }
        }
    }
}
