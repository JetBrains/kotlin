// SKIP_JDK6
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK
// FILE: test.kt
fun box(): String {
    val map = mutableMapOf<Fun, String>()
    val fn = Fun { TODO() }
    return map.computeIfAbsent(fn, { "OK" })
}

// FILE: Fun.java
public interface Fun {
    String invoke(String string);
}