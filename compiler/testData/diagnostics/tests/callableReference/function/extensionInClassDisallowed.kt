// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
class A {
    fun Int.extInt() = 42
    fun A.extA(x: String) = x
    
    fun main() {
        Int::<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>extInt<!>
        A::<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>extA<!>

        eat(Int::<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>extInt<!>)
        eat(A::<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>extA<!>)
    }
}

fun eat(value: Any) {}

fun main() {
    A::<!UNRESOLVED_REFERENCE!>extInt<!>
    A::<!UNRESOLVED_REFERENCE!>extA<!>
}
