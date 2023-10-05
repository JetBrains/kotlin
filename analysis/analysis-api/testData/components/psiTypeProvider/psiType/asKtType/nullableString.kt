// FILE: B.kt
interface B: A {
    fun f<caret_onAirContext>oo(): String?
}
// FILE: A.java
public interface A {
    String fo<caret>o();
}