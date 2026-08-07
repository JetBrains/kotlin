// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// IGNORE_BACKEND: JVM_IR
// ISSUE: KT-86525
// Exposing a constructor with a nullable value class parameter replaces the unboxed '<init>' and its
// 'DefaultConstructorMarker' overload with a single boxed '<init>', instead of adding the boxed one beside
// them. A Kotlin caller compiled against the metadata then fails to link with NoSuchMethodError.
// TODO: Remove if green after the fix

// MODULE: lib
// FILE: lib.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

class Holder @JvmExposeBoxed constructor(val id: Id? = Id("OK")) {
    fun describe(): String = id?.value ?: "null"
}

// MODULE: main(lib)
// FILE: usage.kt
fun testFromKotlin(): String = Holder().describe()

// FILE: Main.java
public class Main {
    public String test() {
        return new Holder(null).describe();
    }
}

// FILE: box.kt
fun box(): String {
    val fromKotlin = testFromKotlin()
    if (fromKotlin != "OK") return "FAIL 1: $fromKotlin"

    val res = Main().test()
    if (res != "null") return "FAIL 2: $res"

    return "OK"
}
