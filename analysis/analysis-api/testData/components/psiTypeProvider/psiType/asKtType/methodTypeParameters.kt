// FILE: B.kt
class B: A<String> {
    override fun <K : String?> f<caret_onAirContext>oo(): K? {
        TODO("Not yet implemented")
    }
}
// FILE: A.java
public interface A<T> {
    <K extends T> K fo<caret>o();
}