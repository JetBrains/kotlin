// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// ISSUE: KT-70617

// FILE: Interface.java
public interface Interface {
    String name();

    enum Impl1 implements Interface {
        Value1,
    }

    enum Impl2 implements Interface {
        Value2,
    }
}

// FILE: main.kt
fun box(): String {
    if (Interface.Impl1.Value1.ordinal != 0) return "Fail 1"
    if (Interface.Impl2.Value2.ordinal != 0) return "Fail 2"

    if (Interface.Impl1.Value1.name != "Value1") return "Fail 3"
    if (Interface.Impl2.Value2.name != "Value2") return "Fail 4"

    return "OK"
}