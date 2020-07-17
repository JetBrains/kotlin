// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
class A {
    fun Int.extInt() = 42
    fun A.extA(x: String) = x
    
    fun main() {
        <!UNRESOLVED_REFERENCE!>Int::extInt<!>
        A::extA

        <!INAPPLICABLE_CANDIDATE!>eat<!>(<!UNRESOLVED_REFERENCE!>Int::extInt<!>)
        eat(A::extA)
    }
}

fun eat(value: Any) {}

fun main() {
    <!UNRESOLVED_REFERENCE!>A::extInt<!>
    <!UNRESOLVED_REFERENCE!>A::extA<!>
}
