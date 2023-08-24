// ISSUE: KT-61095

interface X {
    fun foo(a : Int = 1) {}
}

interface Y {
    fun foo(a : Int = 1) {}
}

object YImpl : Y

<!DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>class Z1<!> : X, Y by YImpl {}
<!DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE!>object Z1O<!> : X, Y by YImpl {}
