// FILE: B.kt
interface B: A<String> {
    fun foo(): String?
}
// FILE: A.java
public interface A<T> {
    T f<caret>oo();
}