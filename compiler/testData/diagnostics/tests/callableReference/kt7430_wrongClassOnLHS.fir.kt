// !DIAGNOSTICS: -UNUSED_EXPRESSION

class Unrelated()

class Test(val name: String = "") {
    init {
        <!UNRESOLVED_REFERENCE!>Unrelated::name<!>
        <!UNRESOLVED_REFERENCE!>Unrelated::foo<!>
    }

    fun foo() {}
}
