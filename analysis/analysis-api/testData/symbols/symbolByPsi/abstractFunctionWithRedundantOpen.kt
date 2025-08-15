// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
// KT-80178

interface Foo {
    fun fooDefault(): Unit
    open fun fooWithOpen(): Unit
    open fun fooWithOpenAndBody() { }
}
