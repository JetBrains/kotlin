// FILE: kt45853a.kt
abstract class A {
    open val a: A? get() = null
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
