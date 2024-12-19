// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_TEXT

// MODULE: kotlinlib
// JVM_DEFAULT_MODE: all-compatibility
// FILE: KotlinBase.kt
package kotlinlib

interface NewBase

// MODULE: javalib(kotlinlib)
// FILE: JavaInt.java
package javalib;
import kotlinlib.NewBase;

public interface JavaInt extends NewBase {
    public default Integer foo() {return 1;}
}


// MODULE: lib(javalib, kotlinlib)
// JVM_DEFAULT_MODE: all-compatibility
// FILE: lib.kt
import javalib.JavaInt

interface Base: JavaInt {
    override foo(): Int? = 2
}

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
    return if(a.foo() == 3) { "OK" } else { "Fail" }
}


// java.lang.IllegalStateException: Unexpected member: class org.jetbrains.kotlin.fir.declarations.impl.FirDanglingModifierListImpl


// IrBlackBoxCodegenWithIrInlinerTestGenerated
// FirPsiBlackBoxCodegenTestGenerated