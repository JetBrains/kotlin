// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// TARGET_BACKEND: JVM

// MODULE: lib-common
expect class A {
    fun push(x: String)
    fun pop(): String
}

// MODULE: lib-platform()()(lib-common)
// FILE: JavaDeque.java
import java.util.ArrayDeque;

public class JavaDeque {
    private final ArrayDeque<String> deque = new ArrayDeque<>();

    public void push(String x) {
        deque.push(x);
    }

    public String pop() {
        return deque.pop();
    }
}

// FILE: libPlatform.kt
actual typealias A = JavaDeque

// MODULE: app-common(lib-common)
fun testCommon(a: A): String {
    a.push("OK")
    return a.pop()
}

// MODULE: app-platform(lib-platform)()(app-common)
fun box(): String {
    val a = A()
    val r = testCommon(a)
    return if (r == "OK") "OK" else "FAIL"
}
