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
    jc.<!NONE_APPLICABLE!>greet<!>("foo", language = "language")
}