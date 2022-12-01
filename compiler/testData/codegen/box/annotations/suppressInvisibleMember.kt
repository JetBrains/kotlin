// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// FIR_STATUS: KT-55026
// ISSUE: KT-55026

// MODULE: lib
interface Base {
    val x: String
}

internal class Some(override val x: String) : Base
internal class Other(override val x: String) : Base

// MODULE: main(lib)
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

internal fun Some(): Base = Some("K")
internal fun foo(): Base = Other("O")

fun box(): String = foo().x + Some().x
