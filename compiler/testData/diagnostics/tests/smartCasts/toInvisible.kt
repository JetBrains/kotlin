// SKIP_TXT

// FILE: a/A.java
package a;
public interface A {
    B b();
}

// FILE: a/B.java
package a;
public interface B {
    void bar();
}

// FILE: a/AImpl.java
package a;

public class AImpl implements A {
    @Override
    public BImpl b() {
        return new BImpl();
    }
}

// FILE: a/BImpl.java
package a;

class BImpl implements B {
    @Override
    public void bar() {}
}

// FILE: main.kt
import a.A
import a.AImpl

fun test1(a: A) {
    if (a is AImpl) {
        (a as A).b().bar() // OK
        <!INACCESSIBLE_TYPE!><!DEBUG_INFO_SMARTCAST!>a<!>.b()<!>.<!INVISIBLE_MEMBER!>bar<!>()
    }
}

fun test2(aImpl: AImpl) {
    val a: A = aImpl
    (a <!USELESS_CAST!>as A<!>).b().bar() // OK
    a.b().bar() // Works at FE1.0, fails at FIR
}
