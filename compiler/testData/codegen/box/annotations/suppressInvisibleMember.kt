// TARGET_BACKEND: JVM_IR
// ISSUE: KT-55026

// MODULE: lib
// FILE: lib.kt

interface Base {
    val x: String
}

internal class Some(override val x: String) : Base
internal class Other(override val x: String) : Base

// MODULE: main(lib)
// FILE: main.kt

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

internal fun Some(): Base = Some("K")
internal fun foo(): Base = Other("O")

fun box(): String = foo().x + Some().x
