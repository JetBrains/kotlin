// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// FIR_STATUS: accesses companion property backing field statically and fails (does not work in K1/JVM too)

// FILE: Base.java
public class Base {
    protected String TAG = "OK";
}

// FILE: Sub.kt

class Sub : Base() {
    companion object {
        val TAG = "FAIL"
    }

    fun log() = TAG
}

fun box() = Sub().log()
