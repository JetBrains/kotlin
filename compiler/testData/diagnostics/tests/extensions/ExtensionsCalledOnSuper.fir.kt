interface T {
    fun foo() {}
    fun buzz() {}
    fun buzz1(i : Int) {}
}

fun T.bar() {}

fun T.buzz() {}
fun T.buzz1() {}

class C : T {
    fun test() {
        fun T.buzz() {}
        fun T.buzz1() {}
        super.foo() // OK
        super.<!UNRESOLVED_REFERENCE!>bar<!>() // Error
        super.buzz() // OK, resolved to a member
        super.buzz1<!NO_VALUE_FOR_PARAMETER!>()<!> // Resolved to an extension
        super.buzz1(<!ARGUMENT_TYPE_MISMATCH!>""<!>) // Resolved to a member
    }
}