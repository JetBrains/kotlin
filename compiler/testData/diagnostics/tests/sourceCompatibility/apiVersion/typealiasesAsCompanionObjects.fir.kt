// !API_VERSION: 1.0

class C {
    @SinceKotlin("1.1")
    companion object {
        val x = 42
    }
}

typealias CA = C

val test1 = CA
val test2 = CA.Companion
val test3 = CA.<!UNRESOLVED_REFERENCE!>x<!>
val test4 = CA.Companion.x
