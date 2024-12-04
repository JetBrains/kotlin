// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// MODULE: separate

// FILE: JavaSeparate1.java
public class JavaSeparate1  {
    protected int a = 22;
    protected void foo(){}
}

// FILE: JavaSeparate2.java
public class JavaSeparate2 {
    int a = 23;
    void foo(){}
}

// FILE: JavaSeparate3.java
public class JavaSeparate3 {
    private int a = 24;
    private void foo(){}
}

// MODULE: main(separate)

// FILE: Java1.java
public class Java1 extends JavaSeparate1 {
    public int a = 12;
    public void foo() {}
}

// FILE: Java2.java
public class Java2 extends JavaSeparate2 {
    public int a = 13;
    public void foo() {}
}

// FILE: Java3.java
public class Java3 extends JavaSeparate3 {
    protected int a = 14;
    protected void foo(){}
}

// FILE: Java4.java
public class Java4 extends JavaSeparate2 {
    protected int a = 15;
    protected void foo(){}
}

// FILE: Java5.java
public class Java5 extends JavaSeparate2 {
    private int a = 16;
    private void foo() {}
}

// FILE: test.kt
class A : Java1()  //public + protected

class B : Java1() {
    override fun foo() {}
}

class C : Java2()  //public + default

class D : Java2() {
    override fun foo() {}
}

class E : Java3()  //protected + private

class F : Java3() {
    public override fun foo() {}
}

class G : Java4()  //protected + default

class H : Java4() {
    public override fun foo() {}
}

class I : Java5()  //private + default

class J : Java5() {
    val a : Int = 10
    fun foo() {}
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J) {
    a.a
    a.foo()
    b.a
    b.foo()
    c.a
    c.foo()
    d.a
    d.foo()
    e.a
    e.foo()
    f.a
    f.foo()
    g.a
    g.foo()
    h.a
    h.foo()
    j.a
    j.foo()
}
