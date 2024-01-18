// FILE: B.kt
class B: A<String> {
    override fun <K : String?> foo(): K? {
        TODO("Not yet implemented")
    }
}
// FILE: A.java
public interface A<T> {
    <K extends T> K fo<caret>o();
}