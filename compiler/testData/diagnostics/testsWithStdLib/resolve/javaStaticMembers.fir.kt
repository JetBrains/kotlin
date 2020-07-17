// FILE: Test.java
public class Test {
    public static final String FOO = "test";
}

// FILE: test.kt
fun ff() {
    val a = Test.FOO
    val b = Test?.<!UNRESOLVED_REFERENCE!>FOO<!>
    System.out.println(a + b)
    System?.<!UNRESOLVED_REFERENCE!>out<!>.<!UNRESOLVED_REFERENCE!>println<!>(a + b)
}
