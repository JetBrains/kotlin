// FILE: A.java
public enum A {
    ENTRY,
    ANOTHER;

    public String ENTRY = "";
}

// FILE: test.kt

fun main() {
    val c: A = A.ENTRY
    val c2: String? = c.ENTRY
    val c3: String? = A.ANOTHER.ENTRY
}
