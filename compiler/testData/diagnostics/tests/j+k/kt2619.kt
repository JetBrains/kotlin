// FIR_IDENTICAL
// LANGUAGE: +WarnAboutNonExhaustiveWhenOnAlgebraicTypes
// FILE: foo.kt
fun main() {
    val c: Type
    <!NON_EXHAUSTIVE_WHEN_STATEMENT!>when<!> (<!UNINITIALIZED_VARIABLE!>c<!>)  {

    }
}



//FILE: Type.java
public enum Type {
    TYPE,
    NO_TYPE;
}
