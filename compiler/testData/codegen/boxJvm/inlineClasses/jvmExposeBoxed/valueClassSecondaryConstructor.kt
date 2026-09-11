// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// KT-89068: Expose secondary constructor of a value class

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

var initBlockRuns = 0

@JvmInline
value class Explicit(val a: UInt) {
    init {
        initBlockRuns++
    }

    @JvmExposeBoxed
    constructor(param1: UInt, param2: String) : this(param1 + param2.length.toUInt())
}

@JvmExposeBoxed
@JvmInline
value class Container(val a: UInt) {
    constructor(param1: UInt, param2: String) : this(param1 + param2.length.toUInt())
}

@JvmExposeBoxed("createUInt")
fun create(i: Int): UInt = i.toUInt()

// FILE: Main.java
public class Main {
    public Explicit explicit() {
        return new Explicit(ICKt.createUInt(42), "OK");
    }

    public Container container() {
        return new Container(ICKt.createUInt(42), "OK");
    }
}

// FILE: Box.kt
fun box(): String {
    initBlockRuns = 0

    val e = Main().explicit()
    if (e.a != 44u) return "FAIL explicit: ${e.a}"
    // The init block must run exactly once: in constructor-impl, not again in the exposed constructor.
    if (initBlockRuns != 1) return "FAIL init block runs: $initBlockRuns"

    val c = Main().container()
    if (c.a != 44u) return "FAIL container: ${c.a}"

    return "OK"
}
