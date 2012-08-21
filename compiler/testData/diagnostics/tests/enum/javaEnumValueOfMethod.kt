// FILE: A.java
public enum A {
    ENTRY;
}

// FILE: test.kt
package test

fun main() {
    A.valueOf("ENTRY"): A
}
