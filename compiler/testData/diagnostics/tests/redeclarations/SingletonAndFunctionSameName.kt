fun Foo() = 42

object Foo {
    fun Bar() = 239

    object Bar
}



<!CONFLICTING_OVERLOADS!>fun En()<!> = 239

enum class <!CONFLICTING_OVERLOADS!>En<!> {
    ENTRY,

    SUBCLASS { };

    fun ENTRY() = 42

    fun SUBCLASS() = ENTRY()
}
