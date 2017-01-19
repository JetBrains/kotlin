// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

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
