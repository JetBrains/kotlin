// TARGET_BACKEND: JVM_IR
// ISSUE: KT-54487, KT-63350
// FILE: p/I.java

package p;

public interface I {
    String foo();
}

// FILE: p/I2.java

package p;

public interface I2 extends I {}

// FILE: p/C.java

package p;

public class C {
    static class Impl implements I {
        public String foo() {
            return "OK";
        }
    }

    static class C1Base extends Impl {}
    static class C2Base extends Impl {}

    public static class C1 extends C1Base implements I2 {}
    public static class C2 extends C2Base implements I2 {}

    public static final C1 c1 = new C1();
    public static final C2 c2 = new C2();
}

// FILE: test.kt

import p.C

fun <T> union(a: T, b: T) = a

fun box() =
// receiver type = I2 & C.Impl
// method resolved to C.Impl.foo, but in backend IR the resulting call is I2.foo
    union(C.c1, C.c2).foo()
