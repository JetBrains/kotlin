// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: test/En.java

package test;

public enum En {
    ENTRY;
    
    public static String foo() {
        return "OK";
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box() = test.En.foo()
