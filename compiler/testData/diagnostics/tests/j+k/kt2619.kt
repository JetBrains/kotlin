//FILE: foo.kt
fun main(args: Array<String>) {
    val c: Type
    when (<!UNINITIALIZED_VARIABLE, UNUSED_EXPRESSION!>c<!>)  {

    }
}



//FILE: Type.java
public enum Type {
    TYPE,
    NO_TYPE;
}