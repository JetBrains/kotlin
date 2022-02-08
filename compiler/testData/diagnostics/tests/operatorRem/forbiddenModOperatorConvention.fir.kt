// !LANGUAGE: +ProhibitOperatorMod
// !DIAGNOSTICS: -UNUSED_PARAMETER

object ModAndRem {
    operator fun mod(x: Int) {}
    operator fun rem(x: Int) {}

    operator fun modAssign(x: Int) {}
    operator fun remAssign(x: Int) {}
}

object JustMod {
    operator fun mod(x: Int) {}
    operator fun modAssign(x: Int) {}
}

fun foo() {
    ModAndRem % 1
    ModAndRem.mod(1)
    ModAndRem.rem(1)

    JustMod <!UNRESOLVED_REFERENCE!>%<!> 1
    JustMod.mod(1)

    val r = ModAndRem
    r %= 1
    r.remAssign(1)

    val m = JustMod
    m <!UNRESOLVED_REFERENCE!>%=<!> 1
    m.modAssign(1)
}
