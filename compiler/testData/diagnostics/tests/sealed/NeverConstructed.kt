// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
sealed class Base {
    fun foo() = <!SEALED_CLASS_CONSTRUCTOR_CALL!>Base()<!>
}
