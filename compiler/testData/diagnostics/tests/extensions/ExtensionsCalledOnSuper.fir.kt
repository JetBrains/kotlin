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
        super.bar() // Error
        super.buzz() // OK, resolved to a member
        super.buzz1() // Resolved to an extension
        super.<!INAPPLICABLE_CANDIDATE!>buzz1<!>("") // Resolved to a member
    }
}