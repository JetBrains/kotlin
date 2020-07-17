object A {
    <!CONSTRUCTOR_IN_OBJECT!>constructor()<!>
    init {}
}

enum class B {
    X() {
        <!CONSTRUCTOR_IN_OBJECT, INAPPLICABLE_CANDIDATE!>constructor()<!>
    }
}

class C {
    companion object {
        <!CONSTRUCTOR_IN_OBJECT!>constructor()<!>
    }
}

val anonObject = object {
    <!CONSTRUCTOR_IN_OBJECT!>constructor()<!>
}