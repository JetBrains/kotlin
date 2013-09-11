//KT-3833 Invoke method not working inside class object?
package m

class Either1 {
    class Left() {
        fun match(left: () -> Unit) {
            left()
        }
    }

    inner class Right() {
        fun match(right: () -> Unit) {
            right()
        }
    }
}


class X {
    fun invoke() {
    }
}

object Foo{
    val v : X = X()
}

class C{
    class object {
        fun f(){
            Foo.v()
        }
    }
}

