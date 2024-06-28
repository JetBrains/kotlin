// FIR_IDENTICAL
// TARGET_BACKEND: JVM_IR

// FILE: priv/members/check/MyJClass.java
package priv.members.check;

public class MyJClass {
    public static String O = "O";
    public String k() { return "K"; }
}

// FILE: test.kt
fun test(): String {
    val k: priv.members.check.MyJClass
    k = priv.members.check.MyJClass()

    val o = priv.members.check.MyJClass.O
    return o + k.k()
}
