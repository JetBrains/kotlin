//FILE: foo.kt
fun main(args: Array<String>) {
    val c: Type
    <!NO_ELSE_IN_WHEN!>when<!> (<!UNINITIALIZED_VARIABLE!>c<!>)  {

    }
}



//FILE: Type.java
public enum Type {
    TYPE,
    NO_TYPE;
}