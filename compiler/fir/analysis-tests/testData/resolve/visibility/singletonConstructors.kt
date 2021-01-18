class A {
    companion object Comp {}

    fun foo() {
        <!HIDDEN{LT}!><!HIDDEN{PSI}!>Comp<!>()<!>
    }
}

object B {
    private val x = <!HIDDEN{LT}!><!HIDDEN{PSI}!>B<!>()<!>
}

class D {
    companion object Comp2 {
        operator fun invoke() {}
    }

    fun foo() {
        Comp2()
    }
}

enum class E {
    X {

    };

    fun foo() {
        <!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>X<!>()<!>
    }
}
