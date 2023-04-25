// FIR_IDENTICAL
// ISSUE: KT-57858
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import kotlin.internal.PlatformDependent

interface I {
    @PlatformDependent
    fun f() {}
}

class C : I {
    fun <!VIRTUAL_MEMBER_HIDDEN!>f<!>() {}
}
