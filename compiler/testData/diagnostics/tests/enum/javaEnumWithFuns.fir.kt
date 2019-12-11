// FILE: A.java
public enum A {
    ENTRY,
    ANOTHER;
    
    public String s() {
        return "";
    }
}

// FILE: test.kt

fun main() {
    val c = A.ENTRY
    c.s()
}
