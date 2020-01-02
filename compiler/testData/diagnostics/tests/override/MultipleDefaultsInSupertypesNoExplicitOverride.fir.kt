interface X {
    fun foo(a : Int = 1)
}

interface Y {
    fun foo(a : Int = 1)
}

class Z1 : X, Y {} // BUG
object Z1O : X, Y {} // BUG