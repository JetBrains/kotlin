// FILE: test/A.java
package test;

public enum A {

    ENTRY {
        public String s() {
            return "s";
        }
    };
    public abstract String s();
}

// FILE: test.kt
package test

fun main() {
    A.ENTRY.s(): String?
}
