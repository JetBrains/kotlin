// TARGET_BACKEND: JVM_IR
// Field VS property: case "reference", field is invisible
// FILE: base/BaseJava.java

package base;

public class BaseJava {
    String a = "FAIL";
}

// FILE: Derived.kt

package derived

import base.BaseJava

class Derived : BaseJava() {
    val a = "OK"
}

fun box(): String {
    val d = Derived()
    return d::a.get()
}
