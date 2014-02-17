// FILE: A.java
public enum A {
    ENTRY,
    ANOTHER;
}

// FILE: test.kt
fun main() {
     A.values(): Array<A>
}
