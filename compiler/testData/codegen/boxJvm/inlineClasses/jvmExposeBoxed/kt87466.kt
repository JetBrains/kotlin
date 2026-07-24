// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: 2.kt

package repro

internal fun make(v: V): C = C(v)

fun box(): String {
    make(V(1L))
    return "OK"
}

// FILE: 1.kt

@file:OptIn(ExperimentalStdlibApi::class)

package repro

@JvmInline
internal value class V(val value: Long)

internal class C @JvmExposeBoxed constructor(v: V, f: () -> Unit = {})
