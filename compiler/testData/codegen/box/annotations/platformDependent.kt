// ISSUE: KT-57858

// FILE: Main.kt

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import kotlin.internal.PlatformDependent

interface I {
    @PlatformDependent
    fun f(): String = "FAIL"
}

class C : I {
    fun f() = "OK"
}

fun box() = C().f()