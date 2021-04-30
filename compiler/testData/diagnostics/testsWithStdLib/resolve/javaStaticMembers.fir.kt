// FILE: Test.java
public class Test {
    public static final String FOO = "test";
}

// FILE: test.kt
fun ff() {
    val a = Test.FOO
    val b = <!NO_COMPANION_OBJECT!>Test<!><!UNEXPECTED_SAFE_CALL!>?.<!><!UNRESOLVED_REFERENCE!>FOO<!>
    System.out.println(a + b)
    <!NO_COMPANION_OBJECT!>System<!><!UNEXPECTED_SAFE_CALL!>?.<!><!UNRESOLVED_REFERENCE!>out<!>.println(a + b)
}
