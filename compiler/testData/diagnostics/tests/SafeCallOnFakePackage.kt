// FILE: Test.java
public class Test {
    public static final String FOO = "test";
}

// FILE: anotherTest.kt
package foo

val s: String = "test"

// FILE: test.kt
fun ff() {
    val a = <!FUNCTION_CALL_EXPECTED!>Test<!>?.<!UNRESOLVED_REFERENCE!>FOO<!>
    val b = <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>foo<!><!UNEXPECTED_SAFE_CALL!>?.<!>s
    <!INVISIBLE_MEMBER, FUNCTION_CALL_EXPECTED!>System<!>?.<!UNRESOLVED_REFERENCE!>out<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>println<!>(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> b)
}