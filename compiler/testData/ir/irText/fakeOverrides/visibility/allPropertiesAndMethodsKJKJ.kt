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
    void foo() {}
}

// MODULE: main
// FILE: Java1.java
public class Java1 {
    public int a = 1;
    public void foo() {}
}

// FILE: Java2.java
public class Java2 extends KotlinClass { }

// FILE: Java3.java
public class Java3 {
    protected int a = 3;
    protected void foo(){}
}

// FILE: Java4.java
public class Java4 extends KotlinClass2 { }

// FILE: Java5.java
public class Java5 {
    int a = 5;
    void foo() {}
}

// FILE: Java6.java
public class Java6 extends KotlinClass3 { }

// FILE: Java7.java
public class Java7 extends KotlinClass4 { }

// FILE: Java8.java
public class Java8 extends KotlinClass5 { }

// FILE: test.kt
class A : Java2()

class B : Java2() {
    override fun foo() {}
    val a = 3
}

class C : Java4()

class D : Java4() {
    public override fun foo() {}
    val a = 3
}

class E : Java6()

class F : Java6() {
    public override fun foo() {}
    val a = 3
}

class G : Java7()

class H : Java7() {
    public override fun foo() {}
    val a = 3
}

class I: Java8()

open class KotlinClass : Java1()

open class KotlinClass2: Java3()

open class KotlinClass3 : Java5()

open class KotlinClass4 : JavaSeparate1()

open class KotlinClass5 : JavaSeparate2()

fun test(a: A, b: B, c: C, d: D, e: E, h: H) {
    a.a
    a.foo()
    b.a
    b.foo()
    c.a
    c.foo()
    d.foo()
    e.a
    e.foo()
    h.a
    h.foo()
}
