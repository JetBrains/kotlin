// !LANGUAGE: +ProhibitOperatorMod
// !DIAGNOSTICS: -UNUSED_PARAMETER

object ModAndRem {
    <!FORBIDDEN_BINARY_MOD!>operator<!> fun mod(x: Int) {}
    operator fun rem(x: Int) {}

    <!FORBIDDEN_BINARY_MOD!>operator<!> fun modAssign(x: Int) {}
    operator fun remAssign(x: Int) {}
}

object JustMod {
    <!FORBIDDEN_BINARY_MOD!>operator<!> fun mod(x: Int) {}
    <!FORBIDDEN_BINARY_MOD!>operator<!> fun modAssign(x: Int) {}
}

fun foo() {
    ModAndRem % 1
    ModAndRem.mod(1)
    ModAndRem.rem(1)

    JustMod <!FORBIDDEN_BINARY_MOD_AS_REM!>%<!> 1
    JustMod.mod(1)

    val r = ModAndRem
    r %= 1
    r.remAssign(1)

    val m = JustMod
    m <!FORBIDDEN_BINARY_MOD_AS_REM!>%=<!> 1
    m.modAssign(1)
}