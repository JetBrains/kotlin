// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_PHASE_SUGGESTION: Cannot serialize error type: ERROR CLASS: self-recursive type parameter X
// FIR_IDENTICAL
// FILE: XYZ.java
public interface XYZ<X extends X> {
    XYZ foo() {}
}

// FILE: main.kt

fun main(xyz: XYZ<*>) = xyz.foo()
