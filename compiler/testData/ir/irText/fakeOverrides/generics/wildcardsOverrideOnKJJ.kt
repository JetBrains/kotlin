// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// FILE: Java1.java
import java.util.*;
public class Java1 {
    public void foo(List<? extends Number> a) { }
    public List<? extends Number> bar(){
        return null;
    }
    public void foo2(List<? super Number> a) { }
    public List<? super Number> bar2(){
        return null;
    }

    public void foo3(List<?> a) {}
    public List<?> bar3(){
        return null;
    }
}

// FILE: Java2.java
public class Java2 extends Java1 { }

// FILE: Java3.java
import java.util.ArrayList;
import java.util.List;

public class Java3 extends Java1 {
    @Override
    public void foo(List<? extends Number> a) { }
    @Override
    public List<? extends Number> bar(){
        return new ArrayList<Integer>(1);
    }
    @Override
    public void foo2(List<? super Number> a) { }
    @Override
    public List<? super Number> bar2(){
        return new ArrayList<Number>(1);
    }
    @Override
    public void foo3(List<?> a) {}
    @Override
    public List<?> bar3(){
        return new ArrayList<Integer>(1);
    }
}

// FILE: 1.kt
class A : Java2()   // Kotlin ← Java1 ←Java2

class B : Java2() {
    override fun foo3(a: MutableList<*>?) { }
    override fun bar3(): MutableList<*> {
        return mutableListOf("3")
    }
}

class C : Java3()   // Kotlin ← Java1(override) ←Java2

class D : Java3() {
    override fun foo2(a: MutableList<in Number>) { }
    override fun bar2(): MutableList<in Number> {
        return mutableListOf(1)
    }
}

fun test(a:A, b: B, c: C, d: D){
    a.foo(mutableListOf(1))
    a.foo(null)
    a.bar()
    a.foo2(mutableListOf(1))
    a.foo2(null)
    a.bar2()
    a.foo3(mutableListOf(1))
    a.foo3(null)
    a.bar3()

    b.foo(mutableListOf(1))
    b.foo(null)
    b.bar()
    b.foo2(mutableListOf(1))
    b.foo2(null)
    b.bar2()
    b.foo3(mutableListOf(1))
    b.foo3(null)
    b.bar3()

    c.foo(mutableListOf(1))
    c.foo(null)
    c.bar()
    c.foo2(mutableListOf(1))
    c.foo2(null)
    c.bar2()
    c.foo3(mutableListOf(1))
    c.foo3(null)
    c.bar3()

    d.foo(mutableListOf(1))
    d.foo(null)
    d.bar()
    d.foo2(mutableListOf(1))
    d.bar2()
    d.foo3(mutableListOf(1))
    d.foo3(null)
    d.bar3()
}