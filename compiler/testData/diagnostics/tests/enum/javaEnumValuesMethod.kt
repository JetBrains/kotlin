// !CHECK_TYPE
// FILE: A.java
public enum A {
    ENTRY,
    ANOTHER;
}

// FILE: test.kt
fun main() {
     checkSubtype<Array<A>>(A.values())
}
