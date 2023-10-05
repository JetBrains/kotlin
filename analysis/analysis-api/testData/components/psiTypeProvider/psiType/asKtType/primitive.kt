// FILE: B.kt
interface B: A {
    fun fo<caret_onAirContext>o(): Int
}
// FILE: A.java
public interface A {
    int fo<caret>o();
}