// WITH_STDLIB
// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// INHERIT_MULTIFILE_PARTS

// FILE: Meters.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Meters(val v: String)

// FILE: one.kt
@file:JvmName("Multi")
@file:JvmMultifileClass
@file:OptIn(ExperimentalStdlibApi::class)

@JvmExposeBoxed
fun twice(m: Meters): Meters = Meters(m.v + m.v)

val prop: Meters
    @JvmExposeBoxed("getPropBoxed")
    get() = Meters("K")

// FILE: two.kt
@file:JvmName("Multi")
@file:JvmMultifileClass
@file:OptIn(ExperimentalStdlibApi::class)

@JvmExposeBoxed("outBoxed")
fun out(): Meters = Meters("O")

// FILE: Main.java
public class Main {
    public String test() {
        return Multi.twice(new Meters("O")).getV() + Multi.outBoxed().getV() + Multi.getPropBoxed().getV();
    }
}

// FILE: Box.kt
fun box(): String {
    val java = Main().test()
    if (java != "OOOK") return "FAIL Java: $java"
    val kotlin = twice(Meters("O")).v + out().v + prop.v
    if (kotlin != "OOOK") return "FAIL Kotlin: $kotlin"
    return "OK"
}
