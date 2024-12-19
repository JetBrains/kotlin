// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_TEXT

// MODULE: kotlinlib
// JVM_DEFAULT_MODE: disable
// FILE: KotlinBase.kt
package kotlinlib

interface NewBase  {
    fun foo(): Int? = 0
}

// MODULE: javalib(kotlinlib)
// FILE: JavaInt.java
package javalib;
import kotlinlib.NewBase;
public interface JavaInt extends NewBase {}


// MODULE: lib(javalib, kotlinlib)
// JVM_DEFAULT_MODE: disable
// FILE: lib.kt
import javalib.JavaInt

interface Base: JavaInt

open class Left : Base


// MODULE: main(lib, javalib, kotlinlib)
// JVM_DEFAULT_MODE: disable
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


// 0 public foo\(\)Ljava/lang/Integer;
// 0 INVOKESPECIAL Right.foo \(\)Ljava/lang/Integer; \(itf\)
// 2 ALOAD 0


// IrBlackBoxCodegenWithIrInlinerTestGenerated
// FirPsiBlackBoxCodegenTestGenerated