// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// ISSUE: KT-85002

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

open class Base {
    fun inherited(id: Id): String = id.value
}

// Exposure works on declarations, not on the inherited surface: 'Derived' declares nothing named 'inherited',
// so the class-level annotation adds no boxed 'inherited' to it. This is by design - the listing has to show a
// boxed 'declared' and no boxed 'inherited'.
@JvmExposeBoxed
class Derived : Base() {
    fun declared(id: Id): String = id.value
}

// FILE: Main.java
public class Main {
    public String test() {
        return new Derived().declared(new Id("OK"));
    }
}

// FILE: Box.kt
fun box(): String {
    val res = Main().test()
    if (res != "OK") return "FAIL 1: $res"

    // The inherited member keeps only its mangled form, so it stays reachable from Kotlin.
    val inherited = Derived().inherited(Id("OK"))
    if (inherited != "OK") return "FAIL 2: $inherited"

    return "OK"
}
