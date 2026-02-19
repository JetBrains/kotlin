// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
// FILE: JavaInterface.java
public interface JavaInterface {
    String getFoo(String a, String b);
    String bar(String a, String b, String c);
}

// FILE: KotlinContextAndExtensionInterface.kt
interface KotlinContextAndExtensionInterface {
    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    val String.foo: String

    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    fun String.bar(b: String): String
}

// FILE: test.kt
interface Intersection : KotlinContextAndExtensionInterface, JavaInterface

interface IntersectionWithOverride : KotlinContextAndExtensionInterface, JavaInterface {
    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    override val String.foo: String
        get() = ""

    <!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
    override fun String.bar(b: String): String
}
