// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// SKIP_KT_DUMP

// FILE: Base.java
public class Base<T> {
    public T f = null;
    public static int s = 0;
}

// FILE: Derived.java
public class Derived extends Base<Integer> {}

// FILE: main.kt
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

fun testNonStatic(b: Base<Int>, d: Derived, i: Impl, o: OtherImpl) {
    // IrGetField
    b.f // Base.f
    d.f // Base.f
    i.f // Base.f
    o.f // Base.f

    // IrSetField
    b.f = 1 // Base.f
    d.f = 1 // Base.f
    i.f = 1 // Base.f
    o.f = 1 // Base.f

    // IrPropertyReference
    b::f // Base.f
    d::f // Derived.f
    i::f // Impl.f
    o::f // OtherImpl.f
}

fun testStatic() {
    // IrGetField
    Base.s // Base.s
    Derived.s // Base.s

    // IrSetField
    Base.s = 1 // Base.s
    Derived.s = 1 // Base.s

    // IrPropertyReference
    Derived::s // Derived.s
}
