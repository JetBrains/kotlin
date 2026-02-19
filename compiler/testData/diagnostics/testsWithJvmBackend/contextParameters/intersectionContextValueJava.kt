// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
// FILE: JavaInterface.java
public interface JavaInterface {
    String foo(String a, String b);
    String getB(String a);
}

// FILE: KotlinContextInterface.kt
interface KotlinContextInterface {
    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    fun foo(b: String): String

    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    val b: String
}

// FILE: test.kt
interface Intersection : KotlinContextInterface, JavaInterface

interface IntersectionWithOverride : KotlinContextInterface, JavaInterface {
    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    override fun foo(b: String): String

    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    override val b: String
        get() = ""
}
