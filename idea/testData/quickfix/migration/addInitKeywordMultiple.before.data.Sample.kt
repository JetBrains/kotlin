annotation class Ann3
annotation class Ann4

class D {
    Ann3 init {

    }
    Ann4 {
        class Q {
            {

            }
        }
    }
}

class E {
    companion object {
        init {

        }
        {}
    }
}

fun foo() = 1
class F {
    val a1 = foo();

    {

    }

    val a2 = foo()

    ;{

    }

    val a3 = foo(); // el
    /* abc */{

    }

    val a4 = foo() // el

    ;/* abc */{

    }

    val a5 = foo()
    /* abc */;{

    }

    val a6 = foo();
    init {

    }
}
