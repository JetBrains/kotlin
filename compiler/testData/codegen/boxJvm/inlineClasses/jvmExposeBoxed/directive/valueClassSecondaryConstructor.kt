// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// JVM_EXPOSE_BOXED
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM_IR
// The whole-module flag exposes value class constructors automatically, but a secondary constructor still
// produces only the static 'constructor-impl' and no '<init>', so it stays unreachable from Java.
// TODO: Remove if green after the fix

// FILE: IC.kt
@JvmInline
value class Secondary(val value: Int) {
    constructor() : this(42)
}

@JvmInline
value class AllDefault(val value: Int = 42)

// FILE: Main.java
public class Main {
    public int secondary() {
        return new Secondary().getValue();
    }

    public int allDefaultPrimary() {
        return new AllDefault().getValue();
    }
}

// FILE: Box.kt
fun box(): String {
    val secondary = Main().secondary()
    if (secondary != 42) return "FAIL 1: $secondary"
    val allDefault = Main().allDefaultPrimary()
    if (allDefault != 42) return "FAIL 2: $allDefault"
    return "OK"
}
