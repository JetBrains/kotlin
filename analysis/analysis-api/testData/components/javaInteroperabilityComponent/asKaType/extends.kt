// FILE: B.kt
interface B: A {
    fun foo(): java.util.List<String?>?
}
// FILE: A.java
public interface A {
    java.util.List<? extends String> fo<caret>o();
}