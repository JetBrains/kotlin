// TARGET_BACKEND: JVM
// IGNORE_CODEGEN_WITH_FIR2IR_FAKE_OVERRIDE_GENERATION
// FILE: Base.java

import org.jetbrains.annotations.NotNull;

public abstract class Base {
    @NotNull
    protected String result = "OK";
}

// FILE: Derived.kt

open class Mid : Base()

class Derived : Mid() {
    fun foo(): String =
        (Derived::result)(this)
}

fun box(): String = Derived().foo()
