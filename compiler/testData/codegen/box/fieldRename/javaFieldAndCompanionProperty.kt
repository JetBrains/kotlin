// TARGET_BACKEND: JVM_IR

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
