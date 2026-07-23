// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: JavaProtected.java
public class JavaProtected {
    protected int a = 3;
    protected void foo() {}
}

// FILE: JavaDefault.java
public interface JavaDefault {
    int a = 2;
    void foo();
}

// FILE: JavaPublic.java
public interface JavaPublic {
    public int a = 1;
    public void foo();
}

// FILE: JavaPrivate.java
public class JavaPrivate  {
    private int a = 2;
    private void foo(){}
}

// FILE: test.kt
abstract class A : JavaPublic, JavaDefault

class B : JavaPublic, JavaDefault {
    override fun foo() {}
    val a = 1
}

class C : JavaPublic, JavaProtected() {
    override fun foo() {}
    val a = 1
}

abstract class D : JavaPublic, JavaPrivate()

class E : JavaPublic, JavaPrivate() {
    override fun foo() {}
    val a = 1
}

class F : JavaProtected(), JavaDefault {
    override fun foo() {}
    val a = 1
}

abstract class G : JavaPrivate(), JavaDefault

class H : JavaPrivate(), JavaDefault {
    override fun foo() {}
    val a = 1
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H) {
    a.foo()
    b.a
    b.foo()
    c.a
    c.foo()
    d.foo()
    e.foo()
    f.a
    f.foo()
    g.foo()
    h.foo()
    h.a
}