//FILE: foo.kt
fun main() {
    val c: Type
    <!NON_EXHAUSTIVE_WHEN!>when<!> (<!UNINITIALIZED_VARIABLE, UNUSED_EXPRESSION!>c<!>)  {

    }
}



//FILE: Type.java
public enum Type {
    TYPE,
    NO_TYPE;
}