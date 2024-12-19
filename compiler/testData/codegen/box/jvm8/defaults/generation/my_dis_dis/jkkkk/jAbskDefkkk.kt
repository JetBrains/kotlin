// TARGET_BACKEND: JVM_IR

// MODULE: javalib
// FILE: JavaInt.java
package javalib;

public interface JavaInt {
    public abstract Integer foo();
}

// MODULE: lib(javalib)
// JVM_DEFAULT_MODE: disable
// FILE: lib.kt
import javalib.JavaInt

interface Base : JavaInt {
    override fun foo(): Int? = null
}

open class Left : Base

// MODULE: main(lib, javalib)
// JVM_DEFAULT_MODE: disable
// FILE: main.kt

interface Right : Base {
    override fun foo(): Int? = 3
}

class Bottom : Right, Left()

fun box(): String {
    val a = Bottom()
    return if(a.foo()!! == 3) { "OK" } else { "Fail" }
}

//1 public foo\(\)Ljava/lang/Integer;
//1 INVOKESPECIAL Right.foo \(\)Ljava/lang/Integer; \(itf\)