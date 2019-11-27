// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: a.kt

inline class IC(val v: Int) {
    fun <T> getT(): T? = null
}

// FILE: UseIC.java

public class UseIC {
    private IC ic = null;

    public static String result() {
        return "OK";
    }
}

// FILE: test.kt

fun box(): String {
    return UseIC.result()
}
