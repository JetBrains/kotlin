// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
// FILE: XYZ.java
public interface XYZ<X extends Y, Y extends Z, Z extends Y> {
    XYZ foo() {}
}

// FILE: main.kt

fun main(xyz: XYZ<*, *, *>) = xyz.foo()