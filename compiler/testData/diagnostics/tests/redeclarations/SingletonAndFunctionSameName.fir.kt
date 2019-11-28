fun Foo() = 42

object Foo {
    fun Bar() = 239

    object Bar
}



fun En() = 239

enum class En {
    ENTRY,

    SUBCLASS { };

    fun ENTRY() = 42

    fun SUBCLASS() = ENTRY()
}
