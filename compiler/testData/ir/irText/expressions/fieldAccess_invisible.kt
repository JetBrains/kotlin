// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// SKIP_KT_DUMP

// FILE: foo/Base.java
package foo;

class Base {
    public int f = 0;
    public static int s = 0;
}

// FILE: foo/Derived.java
package foo;

public class Derived extends Base {}

// FILE: main.kt
import foo.Derived

open class Impl : Derived() {
    fun testClass() {
        // IrGetField
        f // Base.f
        super.f // Base.f

        // IrSetField
        f = 1 // Base.f
        super.f = 1 // Base.f
    }

    fun testClassStatic() {
        // IrGetField
        s // Base.s

        // IrSetField
        s = 1 //
    }
}

class OtherImpl : Impl() {
    fun testOtherClass() {
        // IrGetField
        f // Base.f
        super.f // Base.f

        // IrSetField
        f = 1 // Base.f
        super.f = 1 // Base.f
    }

    fun testOtherClassStatic() {
        // IrGetField
        s // Base.s

        // IrSetField
        s = 1 //
    }
}

fun testNonStatic(d: Derived, i: Impl, o: OtherImpl) {
    // IrGetField
    d.f // Base.f
    i.f // Base.f
    o.f // Base.f

    // IrSetField
    d.f = 1 // Base.f
    i.f = 1 // Base.f
    o.f = 1 // Base.f

    // IrPropertyReference
    d::f // Derived.f
    i::f // Impl.f
    o::f // OtherImpl.f
}

fun testStatic() {
    // IrGetField
    Derived.s // Base.s

    // IrSetField
    Derived.s = 1 // Base.s

    // IrPropertyReference
    Derived::s // Derived.s
}
