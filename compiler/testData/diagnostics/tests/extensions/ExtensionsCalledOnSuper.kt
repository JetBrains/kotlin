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
        <!SUPER_CANT_BE_EXTENSION_RECEIVER!>super<!>.buzz1() // Resolved to an extension
        super.buzz1(<!TYPE_MISMATCH!>""<!>) // Resolved to a member
    }
}