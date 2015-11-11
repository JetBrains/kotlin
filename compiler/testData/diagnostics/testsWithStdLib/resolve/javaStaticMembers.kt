// FILE: Test.java
public class Test {
    public static final String FOO = "test";
}

// FILE: test.kt
fun ff() {
    val a = Test.FOO
    val b = <!FUNCTION_CALL_EXPECTED!>Test<!>?.<!UNRESOLVED_REFERENCE!>FOO<!>
    System.out.println(a + <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>b<!>)
    <!INVISIBLE_MEMBER, FUNCTION_CALL_EXPECTED!>System<!>?.<!UNRESOLVED_REFERENCE!>out<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>println<!>(a + <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>b<!>)
}