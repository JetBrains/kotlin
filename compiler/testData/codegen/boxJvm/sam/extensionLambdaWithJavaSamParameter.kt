// TARGET_BACKEND: JVM
// FILE: Action.java

public interface Action<T> {
    void execute(T t);
}

// FILE: box.kt

val artifactsGetter: String.(Action<Int>) -> String =
    { "OK" }


fun box(): String = "Test".artifactsGetter {}
