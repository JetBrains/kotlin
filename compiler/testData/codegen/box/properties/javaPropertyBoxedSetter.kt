// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: JavaClass.java

public class JavaClass {

    private boolean value;

    public boolean isValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }
}

// FILE: kotlin.kt

fun box(): String {
    val javaClass = JavaClass()

    if (javaClass.isValue != false) return "fail 1"

    javaClass.isValue = true

    if (javaClass.isValue != true) return "fail 2"

    return "OK"
}
