// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
class Foo {
    val p : dynamic = null

    fun f(p: dynamic): dynamic {
        run<dynamic> { "" }
    }
}