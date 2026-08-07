// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

open class Base {
    open fun produce(): Any = "base"
}

// A covariant override specialising to a value class keeps the erased bridge to 'Object' next to its mangled
// form; the exposed variant has to be added beside both of them, not instead of either.
class Derived : Base() {
    @JvmExposeBoxed("produceId")
    override fun produce(): Id = Id("OK")
}

// FILE: Main.java
public class Main {
    public String exposed() {
        return new Derived().produceId().getValue();
    }

    public Object throughBase() {
        return ((Base) new Derived()).produce();
    }
}

// FILE: Box.kt
fun box(): String {
    val res = Main().exposed()
    if (res != "OK") return "FAIL 1: $res"
    val throughBase = Main().throughBase()
    if (throughBase != Id("OK")) return "FAIL 2: $throughBase"
    return "OK"
}
