// FIR_IDENTICAL
// LANGUAGE: +WarnAboutNonExhaustiveWhenOnAlgebraicTypes
// FILE: foo.kt
fun main() {
    val c: Type
    <!NO_ELSE_IN_WHEN!>when<!> (<!UNINITIALIZED_VARIABLE!>c<!>)  {

    }
}



//FILE: Type.java
public enum Type {
    TYPE,
    NO_TYPE;
}
