// CHECK_BYTECODE_TEXT


// MODULE: kotlinlib
// JVM_DEFAULT_MODE: disable
// FILE: KotlinBase.kt
package kotlinlib

interface NewBase  {
    fun foo(): Int? = null
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

interface Base: JavaInt {
    override fun foo(): Int? = 1
}

open class Left : Base





// MODULE: main(lib, javalib, kotlinlib)
// JVM_DEFAULT_MODE: all-compatibility
// FILE: main.kt

interface Right : Base {
    override fun foo(): Int = 3
}

class Bottom : Right, Left()

fun box(): String {
    val a = Bottom()
//    println(a.foo())
    return if(a.foo() == 3) { "OK" } else { "Fail" }
}

// 1 public foo\(\)Ljava/lang/Integer;
// 1 INVOKESPECIAL Right.foo \(\)Ljava/lang/Integer; \(itf\)



// IrBlackBoxCodegenWithIrInlinerTestGenerated
// FirPsiBlackBoxCodegenTestGenerated