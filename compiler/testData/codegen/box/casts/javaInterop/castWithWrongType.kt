// KT-39520
// TARGET_BACKEND: JVM
// FILE: A.java
public class A<T> {
    private T value;
    private A(T x) { value = x; }
    public static <T> T f() {
        return ((A<T>) new A(1)).value;
    }
}

// FILE: test.kt

fun box(): String {
    val x = A.f<String>() as Int
    return if (x == 1) "OK" else "Fail"
}
