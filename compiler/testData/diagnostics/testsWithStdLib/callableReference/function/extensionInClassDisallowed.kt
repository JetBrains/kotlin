// !DIAGNOSTICS: -UNUSED_EXPRESSION
class A {
    fun Int.extInt() = 42
    fun A.extA(x: String) = x
    
    fun main() {
        ::<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>extInt<!>
        ::<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>extA<!>
    }
}

fun main() {
    A::<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>extInt<!>
    A::<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>extA<!>
}