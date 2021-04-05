interface X {
    fun foo(a : Int = 1)
}

interface Y {
    fun foo(a : Int = 1)
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Z1<!> : X, Y {} // BUG
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>object Z1O<!> : X, Y {} // BUG
