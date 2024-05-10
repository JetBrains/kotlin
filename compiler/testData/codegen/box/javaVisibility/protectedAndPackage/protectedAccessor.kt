// TARGET_BACKEND: JVM
// IGNORE_CODEGEN_WITH_FIR2IR_FAKE_OVERRIDE_GENERATION
// MODULE: lib
// FILE: protectedPack/A.java
package protectedPack;

public class A {
    protected final String field;

    public A(String value) {
        field = value;
    }
}

// MODULE: main(lib)
// FILE: B.kt
import protectedPack.A

class B(value: String) : A(value) {
    inner class C : A(field) {
        val result = field
    }
}

fun box(): String = B("OK").C().result
