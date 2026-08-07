// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

// Exposing a secondary constructor must not widen the private primary one: the listing has to keep the
// primary '<init>' private and add a public boxed '<init>' for the secondary only.
class Holder private constructor(val id: Id, val count: Int) {
    @JvmExposeBoxed
    constructor(id: Id) : this(id, 0)

    fun describe(): String = id.value + count
}

// FILE: Main.java
public class Main {
    public String test() {
        return new Holder(new Id("OK")).describe();
    }
}

// FILE: Box.kt
fun box(): String {
    val res = Main().test()
    if (res != "OK0") return "FAIL: $res"
    return "OK"
}
