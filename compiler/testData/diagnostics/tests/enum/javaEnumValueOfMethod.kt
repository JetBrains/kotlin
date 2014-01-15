// FILE: A.java
public enum A {
    ENTRY;
}

// FILE: test.kt
fun main() {
    A.valueOf("ENTRY"): A
}
