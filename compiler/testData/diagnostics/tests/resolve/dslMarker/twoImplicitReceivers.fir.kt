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
                extA()
                extB()

                D().extA()
                D().extB()

                with(D()) {
                    extA()
                    extB()
                }
            }
        }
    }

    foo {
        baz {
            extA()
            D().extA()

            bar {
                extA()
                extB()

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
        foo {
            extA()
            D().extA()

            bar {
                extA()
                extB()

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
                extA()
                extB()

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
