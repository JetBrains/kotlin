// TARGET_BACKEND: JVM

// FILE: Bar.java
public class Bar {
    public static String bar() {
        return null;
    }
}

// FILE: main.kt
fun foo(): Any? {
    return if (true) {
        if (true) {
            Bar.bar()
        } else {
            1
        }
    } else {
        1
    }
}

fun box(): String? {
    foo()
    return "OK"
}
