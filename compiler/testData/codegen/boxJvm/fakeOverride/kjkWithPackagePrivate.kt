// TARGET_BACKEND: JVM_IR

// FILE: a/A.java
package a;

public class A {
    String foo() { return "OK"; }
    String bar() { return "OK"; }
    public static String runFoo(A a) { return a.foo();}
    public static String runBar(A a) { return a.bar();}
}

// FILE: c/C.kt
package c

open class C : a.A() {
    fun bar(): String = "FAIL"
}

// FILE: c/D.java
package c;

public class D extends C {}

// FILE: c/E.kt
package c

open class E : D() {
}


// FILE: box.kt

import a.*
import c.*

fun box(): String {
    if (A.runFoo(A()) != "OK") return "FAIL";
    if (A.runFoo(C()) != "OK") return "FAIL";
    if (A.runFoo(D()) != "OK") return "FAIL";
    if (A.runFoo(E()) != "OK") return "FAIL";
    if (A.runBar(A()) != "OK") return "FAIL";
    if (A.runBar(C()) != "OK") return "FAIL";
    if (A.runBar(D()) != "OK") return "FAIL";
    if (A.runBar(E()) != "OK") return "FAIL";
    return "OK"
}