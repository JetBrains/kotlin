interface T {
    fun foo() {}
    fun buzz() {}
    fun buzz1(i : Int) {}
}

fun T.bar() {}

fun T.<!EXTENSION_SHADOWED_BY_MEMBER!>buzz<!>() {}
fun T.buzz1() {}

class C : T {
    fun test() {
        fun T.<!EXTENSION_SHADOWED_BY_MEMBER!>buzz<!>() {}
        fun T.buzz1() {}
        super.foo() // OK
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.bar() // Error
        super.buzz() // OK, resolved to a member
        super.buzz1(<!NO_VALUE_FOR_PARAMETER!>)<!> // Resolved to a member, but error: no parameter passed where required
    }
}