// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// FULL_JDK

// FILE: Java1.java
public  class Java1 extends A { }

// FILE: Java2.java
import java.util.function.Function;

public interface Java2 {
    public void foo(Function<Integer, Integer> lambda);
    public void foo2(Function<Integer, Integer> lambda);
}

// FILE: 1.kt
import java.util.function.Function

open class A {
    inline fun foo(noinline lambda: (Int) -> Int) {}
    inline fun foo2(crossinline lambda: (Int) -> Int) {}
}

class B : Java1()   //Kotlin ← Java ← Kotlin

abstract class C: Java1(), Java2    //Kotlin ← Java1, Java2 ← Kotlin2

class D: Java1(), Java2 {   //Kotlin ← Java1, Java2 ← Kotlin2
    override fun foo(lambda: Function<Int, Int>?) { }
    override fun foo2(lambda: Function<Int, Int>?) { }
}

abstract class E : A(), Java2   //Kotlin ← Java1, Kotlin2

class F : A(), Java2{
    override fun foo(lambda: Function<Int, Int>?) { }
    override fun foo2(lambda: Function<Int, Int>?) { }
}

fun test(b: B, c: C, d: D, e: E, f: F) {
    b.foo { 1 }
    b.foo2 { 1 }
    c.foo { 1 }
    c.foo2 { 1 }
    d.foo { 1 }
    d.foo2 { 1 }
    e.foo { 1 }
    e.foo2 { 1 }
    f.foo { 1 }
    f.foo2 { 1 }
}