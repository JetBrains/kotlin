// FILE: Test.java
public class Test {
    public static final String FOO = "test";
}

// FILE: anotherTest.kt
package foo

val s: String = "test"

// FILE: test.kt
fun ff() {
    val a = <!NO_COMPANION_OBJECT!>Test<!><!UNEXPECTED_SAFE_CALL!>?.<!>FOO
    val b = <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>foo<!><!UNEXPECTED_SAFE_CALL!>?.<!>s
    <!NO_COMPANION_OBJECT!>System<!><!UNEXPECTED_SAFE_CALL!>?.<!>out.println(a + b)
}
