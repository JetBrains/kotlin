// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: JavaClass.java
import java.util.concurrent.atomic.*;

public class JavaClass {
    public String foo(AtomicInteger i) {
        return "OK";
    }
}

// FILE: test.kt
import JavaClass
import java.util.concurrent.atomic.AtomicInteger

class KotlinClass : JavaClass()

fun box(): String {
    return KotlinClass().foo(AtomicInteger(0))
}