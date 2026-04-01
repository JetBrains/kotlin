// TARGET_BACKEND: JVM
// FILE: J.java
public class J {
    public J getParent() { return null; }
}

// FILE:box.kt
fun box(): String {
    var j: J = J()
    try {
        j = j.parent
        return "FAIL"
    } catch (e: NullPointerException) {
        return "OK"
    }
}