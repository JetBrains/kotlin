// LANGUAGE: -ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty
// ISSUE: KT-52338

// FILE: Base.java
public class Base {
    protected String TAG = "OK";

    public String foo() {
        return TAG;
    }
}

// FILE: Sub.kt

class Sub : Base() {
    companion object {
        val TAG = "FAIL"
    }

    fun log() = <!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>TAG<!>

    fun logReference() = this::<!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>TAG<!>.get()

    fun logAssignment(): String {
        <!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>TAG<!> = "12"
        if (foo() != "12") return "Error writing: ${foo()}"
        return "OK"
    }
}
