// SKIP_KT_DUMP
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

// FILE: test.kt
class A : Java1()

class B: Java1(){
    override fun foo(a: MutableList<out Number>?) { }
    override fun bar(): MutableList<out Number> {
        return null!!
    }

    override fun foo2(a: MutableList<in Number>?) { }
    override fun bar2(): MutableList<in Number> {
        return null!!
    }

    override fun foo3(a: MutableList<*>?) { }
    override fun bar3(): MutableList<*> {
        return null!!
    }
}

fun test(a: A, b: B){
    a.foo(null)
    a.foo(mutableListOf(null))
    a.foo(listOf(1))
    a.bar()
    a.foo2(null)
    a.foo2(mutableListOf(null))
    a.foo2(mutableListOf(1.1))
    a.bar2()
    a.foo3(null)
    a.foo3(mutableListOf(null))
    a.foo3(listOf(null))
    a.foo3(listOf(""))
    a.bar3()
    b.foo(null)
    b.foo(mutableListOf(1))
    b.bar()
    b.foo2(null)
    b.foo2(mutableListOf(null))
    b.foo2(mutableListOf(1.1))
    b.bar2()
    b.foo3(null)
    b.foo3(mutableListOf(null))
    b.foo3(mutableListOf(""))
    b.bar3()
}