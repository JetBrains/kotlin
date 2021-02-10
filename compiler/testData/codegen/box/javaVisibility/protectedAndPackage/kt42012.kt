// IGNORE_BACKEND: JVM
// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: test/Parent.java

package test;

public class Parent {
    protected String qqq = "";

    public String getQqq() {
        return qqq;
    }
}

// MODULE: main(lib)
// FILE: 1.kt

import test.Parent

open class Child : Parent() {
    inner class QQQ {
        fun z(x: Parent?) {
            x as Child
            val q = x.qqq
            x.qqq = q + "OK"
        }
    }
}

fun box(): String {
    val c = Child()
    val d = c.QQQ()
    d.z(c)
    return c.qqq
}
