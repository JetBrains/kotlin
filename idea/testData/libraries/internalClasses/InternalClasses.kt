package test

trait T {
    fun foo() {
    }

    fun boo()
}

trait TT : T {
}

fun f() {
    var i = 0
    val myLocalFun = {
        ++i
    }

    class MyLocalClass {
    }
}

class A {
    class B {
        class C {
        }
    }

    inner class C {
    }
}
