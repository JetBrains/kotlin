// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FILE: JClass.java
public class JClass {
    public String field;

    public JClass(String field){
        this.field = field;
    }
}

// FILE: main.kt
inline fun call(s: () -> String): String {
    return s()
}

fun box(): String {
    return call(JClass("OK")::field)
}