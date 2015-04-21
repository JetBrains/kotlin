// !CHECK_TYPE
// FILE: A.java
public enum A {
    ENTRY;
}

// FILE: test.kt
fun main() {
    checkSubtype<A>(A.valueOf("ENTRY"))
}
