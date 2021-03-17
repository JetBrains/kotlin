// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
class A {
    fun Int.extInt() = 42
    fun A.extA(x: String) = x
    
    fun main() {
        <!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>Int::extInt<!>
        <!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>A::extA<!>

        eat(<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>Int::extInt<!>)
        eat(<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>A::extA<!>)
    }
}

fun eat(value: Any) {}

fun main() {
    <!UNRESOLVED_REFERENCE!>A::extInt<!>
    <!UNRESOLVED_REFERENCE!>A::extA<!>
}
