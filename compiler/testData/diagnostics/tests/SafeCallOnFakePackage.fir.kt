// FILE: Test.java
public class Test {
    public static final String FOO = "test";
}

// FILE: anotherTest.kt
package foo

val s: String = "test"

// FILE: test.kt
fun ff() {
    val a = Test<!UNEXPECTED_SAFE_CALL!>?.<!><!UNRESOLVED_REFERENCE!>FOO<!>
    val b = foo<!UNEXPECTED_SAFE_CALL!>?.<!><!UNRESOLVED_REFERENCE!>s<!>
    System<!UNEXPECTED_SAFE_CALL!>?.<!><!UNRESOLVED_REFERENCE!>out<!>.println(a + b)
}
