// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75705
// FILE: JavaClass.java
public class JavaClass {
    public static Boolean staticField = false;

    public String field = false;
}

// FILE: test.kt
fun test() {
    JavaClass::staticField.isInitialized

    JavaClass()::field.isInitialized
}
