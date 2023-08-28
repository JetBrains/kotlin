// TARGET_BACKEND: JVM
// IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION: KT-61370
// ISSUE: KT-51194

// MODULE: coreLib_1
// FILE: Base.java
public interface Base {
    Object foo();
}

// MODULE: lib(coreLib_1)
// FILE: Derived.java
public abstract class Derived implements Base {
    @Override
    public Object foo() {
        return null;
    }
}

// MODULE: coreLib_2
// FILE: Base.java
public interface Base {
    <T> T foo();
}

// MODULE: main(coreLib_2, lib)
// FILE: main.kt
class Implementation : Derived()

fun box() = "OK"
