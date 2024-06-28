// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB

// FILE: Java1.java
import java.util.*;

public class Java1 {
    public List a = new ArrayList();
    public void foo(List a) {};
    public List bar() { return null; };
}

// FILE: 1.kt

class A : Java1()

class B : Java1() {
    override fun bar(): MutableList<Any> {
        return null!!
    }
    override fun foo(a: MutableList<Any?>) { }
}

class C : Java1() {
    override fun foo(a: List<*>?) { }
}

fun test(a: A, b: B, c: C){
    val k: List<Any?> = a.bar()
    a.foo(null)
    a.foo(mutableListOf(null))
    val k2: List<Any> = b.bar()
    b.foo(mutableListOf(null))
    c.foo(null)
    c.foo(listOf(null))
}