// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

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

fun box(): String {
    if (Sub().log() != "OK") return Sub().log()
    if (Sub().logReference() != "OK") return Sub().logReference()
    return Sub().logAssignment()
}
