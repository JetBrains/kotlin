interface X {
    fun foo(a : Int = 1)
}

interface Y {
    fun foo(a : Int = 1)
}

class Z : X, Y {
    fun <!VIRTUAL_MEMBER_HIDDEN!>foo<!>(<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES!>a : Int<!>) {}
}

object ZO : X, Y {
    fun <!VIRTUAL_MEMBER_HIDDEN!>foo<!>(<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES!>a : Int<!>) {}
}