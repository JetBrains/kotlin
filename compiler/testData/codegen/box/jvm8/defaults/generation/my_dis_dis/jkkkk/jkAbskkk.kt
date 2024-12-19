// TARGET_BACKEND: JVM_IR

// MODULE: javalib
// FILE: JavaInt.java
package javalib;

public interface JavaInt {}

// MODULE: lib(javalib)
// JVM_DEFAULT_MODE: disable
// FILE: lib.kt
import javalib.JavaInt

interface Base : JavaInt {
    fun foo(): Int?
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

// ABSTRACT_MEMBER_NOT_IMPLEMENTED: Class 'Left' is not abstract and does not implement abstract member