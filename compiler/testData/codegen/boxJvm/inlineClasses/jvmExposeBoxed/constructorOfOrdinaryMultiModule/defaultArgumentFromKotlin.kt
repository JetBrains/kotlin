// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JVM_IR
// ISSUE: KT-86525
// Exposing a constructor that has default arguments drops the 'DefaultConstructorMarker' overload the
// metadata still advertises, so a Kotlin caller in another module fails to link with
// NoSuchMethodError: Holder.<init>(LId;IILkotlin/jvm/internal/DefaultConstructorMarker;)V
// TODO: Remove if green after the fix

// MODULE: lib
// FILE: lib.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

class Holder @JvmExposeBoxed constructor(val id: Id = Id("OK"), val count: Int = 0) {
    fun describe(): String = id.value + count
}

// MODULE: main(lib)
// FILE: usage.kt
// A Kotlin caller in another module resolves the '$default' constructor from metadata and links against the
// binary produced above, so exposure has to be purely additive: omitting a default argument must keep working.
fun testFromKotlin(): String {
    val all = Holder().describe()
    if (all != "OK0") return "FAIL omitted: $all"

    val partial = Holder(Id("OK")).describe()
    if (partial != "OK0") return "FAIL partial: $partial"

    return "OK"
}

// FILE: Main.java
public class Main {
    public String test() {
        return new Holder(new Id("OK"), 0).describe();
    }
}

// FILE: box.kt
fun box(): String {
    val fromKotlin = testFromKotlin()
    if (fromKotlin != "OK") return fromKotlin

    val res = Main().test()
    if (res != "OK0") return "FAIL: $res"

    return "OK"
}
