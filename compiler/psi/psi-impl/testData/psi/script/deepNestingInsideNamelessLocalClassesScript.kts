// COMPILATION_ERRORS
package one.two

fun foo() {
    class Local {
        object {
            class A {
                object {
                    class B
                }
            }
        }
    }

    class {
        class Nested {
            class {
                class C
            }
        }
    }
}

object {
    fun bar() {
        class Local {
            object {
                class D
            }
        }
    }

    val property = object {
        class E
    }

    init {
        class {
            class F
        }
    }
}
