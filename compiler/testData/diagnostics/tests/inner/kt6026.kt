// FIR_IDENTICAL
// KT-6026 Exception on instantiating a nested class in an anonymous object

val oo = object {
    // Forbidden in KT-13510
    <!NESTED_CLASS_NOT_ALLOWED!>class Nested<!>

    fun f1() = Nested(<!TOO_MANY_ARGUMENTS!>11<!>)
}
