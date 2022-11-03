// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
// FILE: XYZ.java
public interface XYZ<X extends X> {
    XYZ foo() {}
}

// FILE: main.kt

fun main(xyz: XYZ<*>) = xyz.foo()