// TARGET_BACKEND: JVM_IR

// MODULE: javalib
// FILE: JavaInt.java
package javalib;

public interface JavaInt {
    public default Integer foo() { return 2;}
}

// MODULE: lib(javalib)
// JVM_DEFAULT_MODE: all-compatibility
// FILE: lib.kt
import javalib.JavaInt

open class Left : JavaInt



// MODULE: main(lib, javalib)
// JVM_DEFAULT_MODE: all
// FILE: main.kt
import javalib.JavaInt

interface Right : JavaInt {
    override fun foo(): Int? = 3
}

class Bottom : Right, Left()

fun box(): String {
    val a = Bottom()
    return if(a.foo()!! == 3) { "OK" } else { "Fail" }
}



// IrBlackBoxCodegenWithIrInlinerTestGenerated
// FirPsiBlackBoxCodegenTestGenerated