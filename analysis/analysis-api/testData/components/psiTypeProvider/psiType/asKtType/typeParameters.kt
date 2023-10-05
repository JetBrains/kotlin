// FILE: B.kt
interface B: A<String> {
    fun f<caret_onAirContext>oo(): String?
}
// FILE: A.java
public interface A<T> {
    T f<caret>oo();
}