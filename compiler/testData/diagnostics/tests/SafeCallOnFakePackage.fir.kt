// FILE: Test.java
public class Test {
    public static final String FOO = "test";
}

// FILE: anotherTest.kt
package foo

val s: String = "test"

// FILE: test.kt
fun ff() {
    val a = Test?.<!UNRESOLVED_REFERENCE!>FOO<!>
    val b = foo?.<!UNRESOLVED_REFERENCE!>s<!>
    System?.<!UNRESOLVED_REFERENCE!>out<!>.<!UNRESOLVED_REFERENCE!>println<!>(a + b)
}
