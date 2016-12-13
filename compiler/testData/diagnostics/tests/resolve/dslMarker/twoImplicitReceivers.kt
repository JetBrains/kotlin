// !DIAGNOSTICS: -UNUSED_PARAMETER
@DslMarker
@Target(AnnotationTarget.TYPE)
annotation class Ann1

@DslMarker
@Target(AnnotationTarget.TYPE)
annotation class Ann2

class A {
    fun D.extA() {}
}

class B {
    fun D.extB() {}
}

class D

fun foo(x: (@Ann1 A).() -> Unit) {}
fun bar(x: (@Ann2 B).() -> Unit) {}
fun baz(x: (@Ann1 D).() -> Unit) {}

fun test() {
    foo {
        bar {
            baz {
                <!DSL_SCOPE_VIOLATION!>extA<!>()
                extB()

                D().<!DSL_SCOPE_VIOLATION!>extA<!>()
                D().extB()

                with(D()) {
                    <!DSL_SCOPE_VIOLATION!>extA<!>()
                    extB()
                }
            }
        }
    }

    foo {
        baz {
            <!DSL_SCOPE_VIOLATION!>extA<!>()
            D().<!DSL_SCOPE_VIOLATION!>extA<!>()

            bar {
                <!DSL_SCOPE_VIOLATION!>extA<!>()
                extB()

                D().<!DSL_SCOPE_VIOLATION!>extA<!>()
                D().extB()

                with(D()) {
                    <!DSL_SCOPE_VIOLATION!>extA<!>()
                    extB()
                }
            }
        }
    }

    baz {
        foo {
            <!DSL_SCOPE_VIOLATION!>extA<!>()
            D().extA()

            bar {
                <!DSL_SCOPE_VIOLATION!>extA<!>()
                <!DSL_SCOPE_VIOLATION!>extB<!>()

                D().extA()
                D().extB()

                with(D()) {
                    extA()
                    extB()
                }
            }
        }
    }

    baz {
        bar {
            extB()

            D().extB()

            foo {
                <!DSL_SCOPE_VIOLATION!>extA<!>()
                <!DSL_SCOPE_VIOLATION!>extB<!>()

                D().extA()
                D().extB()

                with(D()) {
                    extA()
                    extB()
                }
            }
        }
    }
}
