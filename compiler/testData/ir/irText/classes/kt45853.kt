// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// SKIP_KT_DUMP
// DUMP_EXTERNAL_CLASS: X
// DUMP_EXTERNAL_CLASS: AX
// FILE: kt45853.kt

abstract class A {
    abstract val a: A?
}

//Fir doesn't treat B.getA as an override, because it is not return-type compatible with AX.getA
// Which might be correct behaivour. So disable fir till KT-46042
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
