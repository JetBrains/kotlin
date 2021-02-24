class A {
    companion object Comp {}

    fun foo() {
        <!HIDDEN!>Comp<!>()
    }
}

object B {
    private val x = <!HIDDEN!>B<!>()
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
        <!UNRESOLVED_REFERENCE!>X<!>()
    }
}
