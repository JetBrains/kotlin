// TARGET_BACKEND: JVM
// MODULE: library
// JVM_DEFAULT_MODE: disable
// FILE: a.kt
package base

interface A {
    fun f(): String = "Fail"
}

open class B : A

// MODULE: main(library)
// JVM_DEFAULT_MODE: enable
// FILE: C.java
import base.A;

public interface C extends A {
    default String f() {
        return "OK";
    }
}

// FILE: source.kt
import base.*

class D : B(), C

fun box(): String = D().f()
