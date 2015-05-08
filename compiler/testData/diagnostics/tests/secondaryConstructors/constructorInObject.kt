object A {
    <!SECONDARY_CONSTRUCTOR_IN_OBJECT!>constructor()
    <!>init {}
}

enum class B {
    X() {
        <!SECONDARY_CONSTRUCTOR_IN_OBJECT!>constructor()
 <!>}
}

class C {
    companion object {
    <!SECONDARY_CONSTRUCTOR_IN_OBJECT!>constructor()
 <!>}
}

val anonObject = object {
    <!SECONDARY_CONSTRUCTOR_IN_OBJECT!>constructor()
<!>}
