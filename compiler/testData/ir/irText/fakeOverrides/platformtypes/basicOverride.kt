// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// MODULE: separate
// FILE: J2.java
public class J2 {
    public String nullableString = "";
    public String getNullableString() {
        return nullableString;
    }
    public void foo(String s) {}
}

// MODULE: main
// FILE: J1.java

public class J1 {
    public String nullableString = "";
    public String getNullableString() {
        return nullableString;
    }
    public void foo(String s) {}
}

// FILE: 1.kt

class A : J1()  //Kotlin ← Java

class B : J2()  //Kotlin ← Java (separate module)

class C : J1() {    //Kotlin ← Java with explicit override
    override fun getNullableString(): String {
        return ""
    }
    override fun foo(s: String?) {}
}

class D : J2() {    //Kotlin ← Java with explicit override (separate module)
    override fun getNullableString(): String {
        return ""
    }
    override fun foo(s: String?) {}
}

fun test() {
    val param: String? = "1"

    val k1: String = A().nullableString
    val k2: String? = A().nullableString
    val k3: String = A().getNullableString()
    val k4: Unit = A().foo(param)
    val k5: String = B().nullableString
    val k6: String? = B().nullableString
    val k7: String = B().getNullableString()
    val k8: Unit = B().foo(param)
    val k9: String = C().nullableString
    val k10: String? = C().nullableString
    val k11: String = C().getNullableString()
    val k12: Unit = C().foo(param)
    val k13: String = D().nullableString
    val k14: String = D().nullableString
    val k15: String = D().getNullableString()
    val k16: Unit = D().foo(param)
}