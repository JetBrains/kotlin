// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/JBase.java
package test;

public class JBase {
    public void f() {}
}

// FILE: test/J.java
package test;

public class J extends JBase {
    public String f = "";
}

// FILE: box.kt
package test

import kotlin.reflect.*
import kotlin.test.assertEquals

open class CBase {
    fun f() {}
}

class C : CBase() {
    var f: String = ""
}

fun takeFunction(x: KFunction<*>): KFunction<*> = x
fun takeProperty(x: KProperty<*>): KProperty<*> = x

fun box(): String {
    assertEquals("fun test.C.f(): kotlin.Unit", takeFunction(C::f).toString())
    assertEquals("var test.C.f: kotlin.String", takeProperty(C::f).toString())
    assertEquals("fun test.J.f(): kotlin.Unit", takeFunction(J::f).toString())
    assertEquals("var test.J.f: kotlin.String!", takeProperty(J::f).toString())
    return "OK"
}
