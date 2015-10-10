// FILE: Test.java
public class Test {
    public static final String FOO = "test";
}

// FILE: anotherTest.kt
package foo

val s: String = "test"

// FILE: test.kt
fun ff() {
    val a = Test<!UNEXPECTED_SAFE_CALL!>?.<!>FOO
    val b = foo<!UNEXPECTED_SAFE_CALL!>?.<!>s
    System<!UNEXPECTED_SAFE_CALL!>?.<!>out.println(a + b)
}