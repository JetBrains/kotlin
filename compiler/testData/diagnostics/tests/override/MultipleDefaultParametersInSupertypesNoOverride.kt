interface X {
    fun foo(a : Int = 1)
}

interface Y {
    fun foo(a : Int = 1)
}

class Z : X, Y {
    <!VIRTUAL_MEMBER_HIDDEN!>fun foo(a : Int)<!> {}
}

object ZO : X, Y {
    <!VIRTUAL_MEMBER_HIDDEN!>fun foo(a : Int)<!> {}
}