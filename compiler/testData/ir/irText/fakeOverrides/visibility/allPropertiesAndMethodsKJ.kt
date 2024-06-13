// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// LANGUAGE: +ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty

// MODULE: separate
// FILE: J2.java
public class J2 {
    public int j1 = 1;
    protected int j2 = 2;
    private int j3 = 3;
    int j4 = 4;

    public void funJ1() {}
    protected void funJ2() {}
    private void funJ3() {}
    void funJ4() {}
}

// FILE: Test.kt
class I : J2() {
    internal val j1 = 11
    internal val j2 = 22
    internal val j3 = 33
    internal val j4 = 44
}

// MODULE: main(separate)
// FILE: J.java
public class J {
    public int j1=12;
    protected int j2=23;
    private int j3=34;
    int j4=45;

    public void funJ1() {}
    protected void funJ2() {}
    private void funJ3() {}
    void funJ4() {}
}


// FILE: test.kt
class A: J()

class B : J2()

class C : J() {
    override fun funJ1() { }
    public override fun funJ2() { }
    internal override fun funJ4() { }
}

class D : J2() {
    override fun funJ1() { }
    public override fun funJ2() { }
}

class E: J() {
    public val j1 = 100
    public val j2 = 200
    public val j3 = 300
    public val j4 = 400
}

open class F : J() {
    protected val j1 = 100
    protected val j2 = 200
    protected val j3 = 300
    protected var j4 = 400
}

class G : J() {
    internal val j1 = 100
    internal val j2 = 200
    internal val j3 = 300
    internal var j4 = 400
}

class H : J() {
    private val j1 = 100
    private val j2 = 200
    private val j3 = 300
    private val j4 = 400
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I) {
    a.j1 = 1
    a.j2 = 2
    a.j4 = 3

    a.funJ1()
    a.funJ2()
    a.funJ4()

    b.j1 = 1
    b.funJ1()

    c.j1 = 1
    c.j2 = 2
    c.j4 = 3

    c.funJ1()
    c.funJ2()
    c.funJ4()

    d.j1 = 1
    d.funJ1()
    d.funJ2()

    e.j1
    e.j2
    e.j3
    e.j4

    f.j1
    f.j2
    f.j4

    g.j1
    g.j2
    g.j3
    g.j4

    h.j1
    h.j2
    h.j4

    i.j1
    i.j2
    i.j4
}
