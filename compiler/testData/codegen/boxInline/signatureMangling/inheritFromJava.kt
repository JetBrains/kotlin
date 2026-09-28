// TARGET_BACKEND: JVM
// FILE: Base.java
public class Base {
    public String ok() { return "OK"; }
}

// FILE: Derived.kt
class Derived: Base()

inline fun ok() = Derived().ok()

// FILE: box.kt
fun box() = ok()