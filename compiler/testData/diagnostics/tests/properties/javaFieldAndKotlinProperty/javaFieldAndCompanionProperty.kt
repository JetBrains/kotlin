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

    fun log() = TAG

    fun logReference() = this::TAG.get()

    fun logAssignment(): String {
        TAG = "12"
        if (foo() != "12") return "Error writing: ${foo()}"
        return "OK"
    }
}
