// FILE: B.kt
interface B: A {
    fun foo(): Int
}
// FILE: A.java
public interface A {
    int fo<caret>o();
}