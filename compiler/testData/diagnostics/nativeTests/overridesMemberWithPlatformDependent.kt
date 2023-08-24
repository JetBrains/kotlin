// ISSUE: KT-57858

// FILE: Sub.kt
package kotlin.internal

@Target(AnnotationTarget.FUNCTION)
annotation class PlatformDependent

// FILE: Main.kt
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import kotlin.internal.PlatformDependent

interface I {
    @PlatformDependent
    fun f() {}
}

class C : I {
    fun <!VIRTUAL_MEMBER_HIDDEN!>f<!>() {}
}
