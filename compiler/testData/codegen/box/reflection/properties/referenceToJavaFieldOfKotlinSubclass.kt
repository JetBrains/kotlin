// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// FILE: J.java

public class J extends K {
    public final int value = 42;
}

// FILE: K.kt

open class K

fun box(): String {
    val f = J::value
    val a = J()
    return if (f.get(a) == 42) "OK" else "Fail: ${f.get(a)}"
}
