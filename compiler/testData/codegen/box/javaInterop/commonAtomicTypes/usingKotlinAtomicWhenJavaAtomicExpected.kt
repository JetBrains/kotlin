// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: JavaClass.java
import java.util.concurrent.atomic.*;

public class JavaClass {
    public String foo(AtomicInteger i) {
        return "O";
    }
    public String bar(AtomicIntegerArray i) {
        return "K";
    }
}

// FILE: test.kt
import JavaClass
import kotlin.concurrent.atomics.*

@OptIn(ExperimentalAtomicApi::class)
fun usage(a: JavaClass): String {
    return a.foo(AtomicInt(0).asJavaAtomic()) + a.bar(AtomicIntArray(1).asJavaAtomicArray())
}

fun box(): String {
    return usage(JavaClass())
}