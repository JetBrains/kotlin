// KT-80178

interface Foo {
    val default: String
    open val defaultWithOpen: String
    open val defaultWithOpenAndBody: String get() = ""

    fun fooDefault(): Unit
    open fun fooWithOpen(): Unit
    open fun fooWithOpenAndBody() { }
}
