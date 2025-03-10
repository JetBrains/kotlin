// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75705
// FILE: JavaClass.java
public class JavaClass {
    public static Boolean staticField = false;

    public String field = false;
}

// FILE: test.kt
fun test() {
    JavaClass::staticField.<!LATEINIT_INTRINSIC_CALL_ON_NON_LATEINIT!>isInitialized<!>

    JavaClass()::field.<!LATEINIT_INTRINSIC_CALL_ON_NON_LATEINIT!>isInitialized<!>
}
