// FILE: test/JavaClass.java

package test;

public class JavaClass {
    public enum E { ENTRY }
    
    public static String foo() { return "OK"; }
}

// FILE: 1.kt

package test

fun box(): String {
    return JavaClass.foo()!!
}
