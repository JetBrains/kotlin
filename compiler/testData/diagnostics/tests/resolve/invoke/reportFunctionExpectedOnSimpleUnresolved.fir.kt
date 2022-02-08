object Scope1 {
    val someVar: Any = Any()

    fun foo() {
        <!UNRESOLVED_REFERENCE!>someVar<!>(1)
    }
}

object Scope2 {
    class Foo

    fun use() {
        val foo = Foo()
        <!UNRESOLVED_REFERENCE!>foo<!>()
    }
}
