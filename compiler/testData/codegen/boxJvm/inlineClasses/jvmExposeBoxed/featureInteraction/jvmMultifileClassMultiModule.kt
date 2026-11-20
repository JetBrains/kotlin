// WITH_STDLIB
// TARGET_BACKEND: JVM

// MODULE: lib
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

// MODULE: main(lib)
// FILE: Box.kt
fun box(): String {
    val result = twice(Meters("O")).v + out().v + prop.v
    return if (result == "OOOK") "OK" else "FAIL: $result"
}
