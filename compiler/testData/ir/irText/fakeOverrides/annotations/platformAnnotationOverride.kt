// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// WITH_STDLIB
// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ ISSUE: KT-66048

// FILE: Java1.java
public class Java1 extends KotlinClass { }

// FILE: Java2.java
public class Java2 extends KotlinClass  {
    public String getA() {
        return "2";
    }
    public String getB() {
        return "2";
    }
    public void setC(String text) { }
    public String getC() {
        return "2";
    }
    public void overloadMethod(String first, String second, int third) { }
}

// FILE: Java3.java
interface Java3 {
    String a = "2";
    void foo();
    String getB();
    void setB(String text);
    void setC(String text);
    String getC();
    void overloadMethod(String first, String second, int third);
}

// FILE: 1.kt
open class KotlinClass {
    @JvmField
    val a = "1"

    companion object {
        @JvmStatic
        fun foo() {}
    }

    @JvmName("getBJava")
    fun getB() : String = "b"

    @get:JvmName("getText")
    @set:JvmName("setText")
    var c = "1"

    @JvmOverloads
    open fun overloadMethod(first : String, second : String = "text", third : Int = 10)  { }
}

class A : Java1()   //Kotlin ← Java ← Kotlin

class B : Java1() {
    override fun overloadMethod(first: String, second: String, third: Int) { }
}

class C : Java2()   // Kotlin ← Java (override) ← Kotlin

class D : Java2() {
    override val a: String
        get() = "3"
    override fun overloadMethod(first: String, second: String, third: Int) { }
}

abstract class E : Java1() , Java3   // Kotlin ← Java1, Java2 ← Kotlin2

class F : Java1() , Java3 {
    override fun foo() { }
    override fun setB(text: String) { }
    override fun setC(text: String) { }
    override fun getC(): String {
        return "5"
    }
}

abstract class G : Java1() , KotlinInterface    // Kotlin ← Java, Kotlin2 ← Kotlin3

class H(override val b: String) : Java1() , KotlinInterface {
    override fun foo() { }
    override fun overloadMethod(first: String) { }
}

interface KotlinInterface{
    val a: String
    fun foo()
    val b : String
    var c : String
    fun overloadMethod(first : String)
}

fun test(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H) {
    a.a
    a.c
    a.c = "2"
    a.overloadMethod("")
    b.a
    b.c
    b.c = "2"
    b.overloadMethod("", "2", 3)
    c.a
    c.c
    c.c = "2"
    c.overloadMethod("")
    c.overloadMethod("","",1)
    d.a
    d.c
    d.c = "2"
    d.overloadMethod("")
    d.overloadMethod("","",1)
    e.a
    e.b
    e.c
    e.c = "2"
    e.b = "2"
    e.overloadMethod("")
    f.a
    f.b
    f.c
    f.c = "2"
    f.b = "2"
    f.overloadMethod("")
    g.a
    g.b
    g.c
    g.c = "2"
    g.overloadMethod("")
    h.a
    h.b
    h.c
    h.overloadMethod("")
    h.overloadMethod("", "", 1)
}