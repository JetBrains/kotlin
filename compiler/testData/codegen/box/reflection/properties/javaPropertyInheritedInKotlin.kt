// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// FILE: J.java

public class J {
    public String result = null;
}

// FILE: K.kt

class K : J()

fun box(): String {
    val k = K()
    val p = K::result
    if (p.get(k) != null) return "Fail"
    p.set(k, "OK")
    return p.get(k)
}
