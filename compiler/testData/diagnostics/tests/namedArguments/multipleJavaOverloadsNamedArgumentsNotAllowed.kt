// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// FILE: JavaClass.java
public class JavaClass {
    public String greet(String name, String language) {
        return "Hello, " + name + "!";
    }

    public String greet(String name, String language, String bean) {
        return "Hello, " + name + "!";
    }
}

// FILE: test.kt

fun test(jc: JavaClass) {
    jc.greet("foo", <!NAMED_ARGUMENTS_NOT_ALLOWED!>language<!> = "language")
}