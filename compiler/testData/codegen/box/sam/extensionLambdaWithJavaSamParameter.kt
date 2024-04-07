// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_LIGHT_ANALYSIS
// FILE: Action.java

public interface Action<T> {
    void execute(T t);
}

// FILE: box.kt

val artifactsGetter: String.(Action<Int>) -> String =
    { "OK" }


fun box(): String = "Test".artifactsGetter {}
