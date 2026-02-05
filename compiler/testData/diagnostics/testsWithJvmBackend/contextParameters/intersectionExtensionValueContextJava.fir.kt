// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
// FILE: JavaInterface.java
public interface JavaInterface {
    String getFoo(String a, String b);
    String bar(String a, String b, String c);
}

// FILE: KotlinContextAndExtensionInterface.kt
interface KotlinContextAndExtensionInterface {
    context(a: String)
    val String.foo: String

    context(a: String)
    fun String.bar(b: String): String
}

// FILE: test.kt
<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>interface Intersection : KotlinContextAndExtensionInterface, JavaInterface<!>

interface IntersectionWithOverride : KotlinContextAndExtensionInterface, JavaInterface {
    context(a: String)
    override val String.foo: String
        <!ACCIDENTAL_OVERRIDE!>get() = ""<!>

    context(a: String)
    override <!ACCIDENTAL_OVERRIDE!>fun String.bar(b: String): String<!>
}
