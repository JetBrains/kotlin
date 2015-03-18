annotation class Ann3
annotation class Ann4

class D {
    Ann3 init {

    }
    Ann4 init {
        class Q {
            init {

            }
        }
    }
}

class E {
    companion object {
        init {

        }
        init {}
    }
}

fun foo() = 1
class F {
    val a1 = foo()

    init {

    }

    val a2 = foo()

    init {

    }

    val a3 = foo(); // el
    /* abc */init {

    }

    val a4 = foo() // el

    ;/* abc */

    init {

    }

    val a5 = foo()
    /* abc */

    init {

    }

    val a6 = foo();
    init {

    }
}
