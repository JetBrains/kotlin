trait T {
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
        <!SUPER_IS_NOT_AN_EXPRESSION!>super<!>.bar() // Error
        super.buzz() // OK, resolved to a member
        super.buzz1<!NO_VALUE_FOR_PARAMETER!>()<!> // Resolved to a member, but error: no parameter passed where required
    }
}
