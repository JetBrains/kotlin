// FILE: JClass.java

public class JClass {
    public int field;
}

// FILE: main.kt
fun box(): String {
    return if (call(10, JClass()::field) == 5) "OK" else "fail"
}

inline fun call(p: Int, s: () -> Int): Int {
    return s()
}

// 1 NEW JClass
// 1 NEW