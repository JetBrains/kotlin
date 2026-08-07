// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// IGNORE_BACKEND: JVM_IR
// Exposing a secondary constructor of a value class emits only the static 'constructor-impl', with no
// '<init>' and no diagnostic, so the constructor is unreachable from Java even though it was explicitly
// exposed. 'AllDefault' is the control: an all-default primary constructor does get a '<init>()V'.
// TODO: Remove if green after the fix

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class Secondary @JvmExposeBoxed constructor(val value: Int) {
    @JvmExposeBoxed
    constructor() : this(42)
}

@JvmInline
value class AllDefault @JvmExposeBoxed constructor(val value: Int = 42)

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
