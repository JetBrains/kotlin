// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// FILE: Fun.java
public interface Fun {
    String invoke(String string);
}

// FILE: test.kt
fun box(): String {
    val map = mutableMapOf<Fun, String>()
    val fn = Fun { TODO() }
    return map.computeIfAbsent(fn, { "OK" })
}

