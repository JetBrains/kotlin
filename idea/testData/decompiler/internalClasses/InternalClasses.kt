package test

interface T {
    fun foo() {
    }

    fun boo()
}

interface TT : T {
}

fun f() {
    var i = 0
    val myAnonymousFunction = {
        ++i
    }

    fun myLocalFunction() {
        i++
    }

    class MyLocalClass {
    }

    val myAnonymousObject = object {
    }


    val lambda = { }
    val samWrapper = Thread(lambda)

    val samLambda = Thread { }

    val callableReference = Any::toString
}

class A {
    class B {
        class C {
        }
    }

    inner class C {
    }
}
