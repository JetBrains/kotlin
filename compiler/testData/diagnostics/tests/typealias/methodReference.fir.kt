class C {
    fun foo() {}
}

typealias CA = C

val cf = <!UNRESOLVED_REFERENCE!>CA::foo<!>
