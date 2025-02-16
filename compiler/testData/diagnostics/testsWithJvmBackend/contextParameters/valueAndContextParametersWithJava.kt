// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
// FILE: JavaBase.java
public interface JavaBase {
    public String foo(String a, String b);
}

// FILE: KotlinInterface.kt
import JavaBase

interface KotlinInterface : JavaBase {
    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    fun foo(b: String): String
}