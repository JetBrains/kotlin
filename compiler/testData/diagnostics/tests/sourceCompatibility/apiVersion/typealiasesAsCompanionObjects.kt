// !API_VERSION: 1.0

class C {
    @SinceKotlin("1.1")
    companion object {
        val x = 42
    }
}

typealias CA = C

val test1 = <!NO_COMPANION_OBJECT!>CA<!>
val test2 = CA.<!UNRESOLVED_REFERENCE!>Companion<!>
val test3 = <!API_NOT_AVAILABLE!>CA<!>.x
val test4 = CA.<!UNRESOLVED_REFERENCE!>Companion<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>
