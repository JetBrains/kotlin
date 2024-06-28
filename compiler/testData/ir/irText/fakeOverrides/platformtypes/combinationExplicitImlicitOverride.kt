// FIR_IDENTICAL
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java

public class Java1 {
    public String nullableString = "";
    public String getNullableString() {
        return nullableString;
    }
    public void foo(String s) {}
}

// FILE: 1.kt

open class A: Java1()

class B : A()   //Grandparent(field/method) ← Parent (fake-override) ← Child (fake-override)

open class C : Java1() {
    override fun foo(s: String?) {}
    override fun getNullableString(): String {
        return "C"
    }
}

class D : C()   //Grandparent(field/method) ← Parent (explicit override) ← Child (fake-override)

class E : C() { //Grandparent(field/method) ← Parent (explicit override) ← Child (explicit override)
    override fun foo(s: String?) { }
    override fun getNullableString(): String {
        return "E"
    }
}

fun test(b: B, d: D, e: E){
    b.foo(null)
    b.foo("")
    b.nullableString = "B"
    val k1: String = b.getNullableString()
    val k2: String = b.nullableString

    d.foo(null)
    d.foo("")
    d.nullableString = "C"
    val k3: String = d.getNullableString()
    val k4: String = d.nullableString

    e.foo(null)
    e.foo("")
    e.nullableString = "E"
    val k5: String = e.getNullableString()
    val k6: String = e.nullableString
}