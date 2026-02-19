// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
// FILE: JavaBase.java
public interface JavaBase {
    public String foo(String a, String b);
}

// FILE: KotlinInterface.kt
import JavaBase

interface KotlinInterface : JavaBase {
    context(a: String)
    <!ACCIDENTAL_OVERRIDE!>fun foo(b: String): String<!>
}
