// FILE: Test.java
public class Test {

    public interface I1 {}
    public interface I2 {}
    public interface I3 {}

    public interface I123 extends I1, I2, I3 {}

    public static class Base {
        public <P extends I1 & I2 & I3> void foo(P p) {}
    }

    public static class Derived extends Base {
        @Override
        public <P extends I1 & I3 & I2> void foo(P p) {}
    }

    public static class DerivedRaw extends Base {
        public void foo(I1 p) {}
    }
}

// FILE: main.kt
interface KI1
interface KI2
interface KI12 : KI1, KI2

open class KBase {
    open fun <P> foo()
    where P : KI1, P : KI2 {}
}

class KDerived : KBase() {
    override fun <P> foo()
    where P : KI2, P : KI1 {}
}


fun callJava(derived: Test.Derived, derivedRaw: Test.DerivedRaw, v: Test.I123) {
    derived.foo(v)
    derivedRaw.foo(v)
}

fun callKotlin(derived: KDerived) {
    derived.foo<KI12>()
}
