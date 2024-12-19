// TARGET_BACKEND: JVM_IR

// MODULE: javalib
// FILE: JavaInt.java
package javalib;

public interface JavaInt {
    public default Integer foo() { return 1;}
}

// MODULE: lib(javalib)
// JVM_DEFAULT_MODE: all-compatibility
// FILE: lib.kt
import javalib.JavaInt

interface Base : JavaInt

open class Left : Base



// MODULE: main(lib, javalib)
// JVM_DEFAULT_MODE: all-compatibility
// FILE: main.kt

interface Right : Base {
    override fun foo(): Int? = 3
}

class Bottom : Right, Left()

fun box(): String {
    val a = Bottom()
    return if(a.foo()!! == 3) { "OK" } else { "Fail" }
}

// 0 public foo\(\)Ljava/lang/Integer;
// 0 INVOKESPECIAL Right.foo \(\)Ljava/lang/Integer; \(itf\)
// 2 ALOAD 0