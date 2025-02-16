// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
// FILE: JavaBase.java
public interface JavaBase {
    public String foo(String a, String b);
}

// FILE: KotlinInterface.kt
import JavaBase

interface KotlinInterface : JavaBase {
    <!ACCIDENTAL_OVERRIDE!>context(a: String)
    fun foo(b: String): String<!>
}