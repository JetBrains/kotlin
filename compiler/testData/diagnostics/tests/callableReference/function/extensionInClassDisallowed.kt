// !DIAGNOSTICS: -UNUSED_EXPRESSION
class A {
    fun Int.extInt() = 42
    fun A.extA(x: String) = x
    
    fun main() {
        Int::<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>extInt<!>
        A::<!EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>extA<!>
    }
}

fun main() {
    A::<!MISSING_RECEIVER, EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>extInt<!>
    A::<!MISSING_RECEIVER, EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED!>extA<!>
}
