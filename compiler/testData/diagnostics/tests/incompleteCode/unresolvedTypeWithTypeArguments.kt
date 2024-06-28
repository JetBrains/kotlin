// FIR_IDENTICAL
abstract class MyClass {
    abstract fun <P1> foo(): (P1) -> <!UNRESOLVED_REFERENCE!>Unknown<!><String>

    private fun callTryConvertConstant() {
        <!UNRESOLVED_REFERENCE!>println<!>(foo<String>())
    }
}
