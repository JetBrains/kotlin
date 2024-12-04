// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
import org.jetbrains.annotations.Nullable;

public class Java1 {
    @Nullable
    public String nullableString = "3";
    @Nullable
    public String bar() {
        return nullableString;
    }
    public void foo(@Nullable String s) {}
}

// FILE: Java2.java
public class Java2 extends Java1  {  }

// FILE: Java3.java
public class Java3 extends Java1 {
    @Override
    public String bar() {
        return "2";
    }

    @Override
    public void foo(String s){}
    public String nullableString = "5";
}

// FILE: Java4.java
public interface Java4  {
    public Object bar();

    public void foo(Object s);
    public Object nullableString = "6";
}

// FILE: Java5.java
public class Java5 extends A  { }

// FILE: 1.kt
open class A : Java2()   // Kotlin ← Java1 ← Java2

class B : Java2() {
    override fun bar(): String {
        return "2"
    }
    override fun foo(s: String?) { }
}

class C : Java3()   // Kotlin ← Java1(override) ← Java2

class D : Java3() {
    override fun foo(s: String?) { }
    override fun bar(): String {
        return "3"
    }
}

abstract class E : Java2(), Java4    // Kotlin ← Java1, Java2 ← Java3

class F : Java2(), Java4 {
    override fun foo(s: Any?) { }
}

abstract class G : Java2() , KotlinInterface     //Kotlin ← Java, Kotlin2 ← Java2

class H(override val nullableString: Any) : Java2() , KotlinInterface {
    override fun foo(s: Any) { }
}

class I : Java5()   //Kotlin ← Java ← Kotlin ← Java

interface KotlinInterface {
    fun bar(): Any?
    fun foo(s: Any)
    val nullableString: Any
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I) {
    a.nullableString
    a.foo("")
    a.bar()
    b.nullableString
    b.foo("")
    b.bar()
    c.nullableString
    c.foo("")
    c.bar()
    d.nullableString
    d.foo("")
    d.bar()
    e.nullableString
    e.foo("")
    e.foo(1)
    e.bar()
    f.nullableString
    f.foo("")
    f.foo(1)
    f.bar()
    g.nullableString
    g.foo(1)
    g.bar()
    h.nullableString
    h.foo(1)
    h.bar()
    i.nullableString
    i.foo("")
    i.bar()
}