// TARGET_BACKEND: JVM
// SKIP_KT_DUMP
// DUMP_EXTERNAL_CLASS: X
// DUMP_EXTERNAL_CLASS: AX

// SKIP_SIGNATURE_DUMP
// ^ Fake overrides have divirging @EnhancedNullability in K1 and K2

// FILE: kt45853.kt

abstract class A {
    abstract val a: A?
}

class B() : AX() {
    override fun getA(): X? = super.a
}

// FILE: X.java
public interface X {
    X getA();
}

// FILE: AX.java
public abstract class AX extends A implements X {
    @Override
    public AX getA() {
        return (AX) super.getA();
    }
}
