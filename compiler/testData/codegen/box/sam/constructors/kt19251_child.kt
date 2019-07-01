// SKIP_JDK6
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK
// FILE: test.kt
fun box(): String {
    val map = mutableMapOf<Fun, String>()
    val fn = DerivedFun { TODO() }
    return map.computeIfAbsent(fn, { "OK" })
}

// FILE: Fun.java
public interface Fun {
    String invoke(String string);
}

// FILE: DerivedFun.java
public interface DerivedFun extends Fun {}