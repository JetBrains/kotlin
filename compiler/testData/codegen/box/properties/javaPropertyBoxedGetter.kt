// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// FILE: JavaClass.java

public class JavaClass {

    private Boolean value;

    public Boolean isValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }
}

// FILE: kotlin.kt

fun box(): String {
    val javaClass = JavaClass()

    if (javaClass.isValue != null) return "fail 1"

    javaClass.isValue = false
    if (javaClass.isValue != false) return "fail 2"

    javaClass.isValue = true
    if (javaClass.isValue != true) return "fail 3"

    return "OK"
}
