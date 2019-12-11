interface X {
    fun foo(a : Int = 1)
}

interface Y {
    fun foo(a : Int = 1)
}

class Z : X, Y {
    fun foo(a : Int) {}
}

object ZO : X, Y {
    fun foo(a : Int) {}
}