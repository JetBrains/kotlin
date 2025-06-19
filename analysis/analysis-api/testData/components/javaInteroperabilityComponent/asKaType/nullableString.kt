// FILE: B.kt
interface B: A {
    fun foo(): String?
}
// FILE: A.java
public interface A {
    String fo<caret>o();
}