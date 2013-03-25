// FILE: test/A.java
package test;

public enum A {
    ENTRY;
}

// FILE: test.kt
package test

fun main() {
    A.valueOf("ENTRY"): A
}
