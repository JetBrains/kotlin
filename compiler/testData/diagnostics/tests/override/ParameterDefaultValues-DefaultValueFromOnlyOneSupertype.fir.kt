interface X {
    fun foo(a : Int = 1)
}

interface Y {
    fun foo(a : Int)
}

class Z : X, Y {
    override fun foo(a : Int) {}
}