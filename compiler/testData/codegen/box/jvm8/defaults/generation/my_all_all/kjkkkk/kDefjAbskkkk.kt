// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_TEXT

// MODULE: kotlinlib
// JVM_DEFAULT_MODE: all
// FILE: KotlinBase.kt
package kotlinlib

interface NewBase  {
    fun foo(): Int? = 0
}

// MODULE: javalib(kotlinlib)
// FILE: JavaInt.java
package javalib;
import kotlinlib.NewBase;
public interface JavaInt extends NewBase {
    public abstract Integer foo();
}


// MODULE: lib(javalib, kotlinlib)
// JVM_DEFAULT_MODE: all
// FILE: lib.kt
import javalib.JavaInt

interface Base: JavaInt

open class Left : Base


// MODULE: main(lib, javalib, kotlinlib)
// JVM_DEFAULT_MODE: all
// FILE: main.kt

interface Right : Base {
    override fun foo(): Int? = 3
}

class Bottom : Right, Left()

fun box(): String {
    val a = Bottom()
//    println(a.foo())
    return if(a.foo() == 3) { "OK" } else { "Fail" }
}


// ABSTRACT_MEMBER_NOT_IMPLEMENTED: Class 'Left' is not abstract and does not implement abstract member:


// IrBlackBoxCodegenWithIrInlinerTestGenerated
// FirPsiBlackBoxCodegenTestGenerated