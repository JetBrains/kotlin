// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// KT-80178

interface Foo {
    val default: String
    open val defaultWithOpen: String
    open val defaultWithOpenAndBody: String get() = ""

    fun fooDefault(): Unit
    open fun fooWithOpen(): Unit
    open fun fooWithOpenAndBody() { }
}
