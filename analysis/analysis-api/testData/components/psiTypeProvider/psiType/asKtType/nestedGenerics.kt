// FILE: B.kt
interface B: A {
    fun fo<caret_onAirContext>o(): java.util.List<String?>?
}
// FILE: A.java
public interface A {
    java.util.List<String> fo<caret>o();
}