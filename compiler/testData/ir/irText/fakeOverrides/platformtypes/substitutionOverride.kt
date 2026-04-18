// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// MODULE: separate

// FILE: J2.java
public class J2<T> {
    public T a;

    public void foo(T t) {};

    public T bar() {
        return a;
    };
}

// MODULE: main(separate)

// FILE: J1.java
public class J1<T> {
    public T a;

    public void foo(T t) {};

    public T bar() {
        return a;
    };
}

// FILE: 1.kt

class A : J1<Int>()

class B : J2<Int>()

class C : J1<Int>() {
    override fun bar(): Int {
        return 1
    }
    override fun foo(t: Int?) { }
}

class D : J2<Int>() {
    override fun bar(): Int {
        return 1
    }
    override fun foo(t: Int?) { }
}

fun test() {
    val k1: Int = A().a
    val k2: Unit = A().foo(1)
    val k3: Int = A().bar()
    val k4: Int = B().a
    val k5: Unit = B().foo(2)
    val k6: Int = B().bar()
    val k7: Int = C().a
    val k8: Unit = C().foo(2)
    val k9: Int = C().bar()
    val k10: Int = D().a
    val k11: Unit = D().foo(2)
    val k12: Int = D().bar()
}
