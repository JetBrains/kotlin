// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty
// ISSUE: KT-56386

// FILE: p/PackagePrivateJavaClass.java
package p;

class PackagePrivateJavaClass {
    public String foo = "OK";
}

// FILE: p/JavaWrapper.java
package p;

public class JavaWrapper {
    protected static class JavaDerived extends PackagePrivateJavaClass {}
}

// FILE: test.kt
import p.JavaWrapper

class KotlinWrapper : JavaWrapper() {
    protected class KotlinDerived : JavaDerived() {
        private val <!PROPERTY_HIDES_JAVA_FIELD!>foo<!> = "FAIL"
    }

    fun bar() = KotlinDerived().<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>foo<!>
}

fun box(): String {
    return KotlinWrapper().bar()
}
