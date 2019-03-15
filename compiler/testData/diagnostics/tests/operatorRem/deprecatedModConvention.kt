// !LANGUAGE: -ProhibitOperatorMod
// !DIAGNOSTICS: -UNUSED_PARAMETER

object ModAndRem {
    <!DEPRECATED_BINARY_MOD!>operator<!> fun mod(x: Int) {}
    operator fun rem(x: Int) {}
}

object OldMod {
    <!DEPRECATED_BINARY_MOD!>operator<!> fun mod(x: Int) {}
}

object ModAndRemExtension
<!DEPRECATED_BINARY_MOD!>operator<!> fun ModAndRemExtension.mod(x: Int) {}
operator fun ModAndRemExtension.rem(x: Int) {}

object ModExtension
<!DEPRECATED_BINARY_MOD!>operator<!> fun ModExtension.mod(x: Int) {}

object ModMemberAndRemExtension {
    <!DEPRECATED_BINARY_MOD!>operator<!> fun mod(x: Int) {}
}

operator fun ModMemberAndRemExtension.rem(x: Int) {}

fun foo() {
    ModAndRem % 1
    OldMod <!DEPRECATED_BINARY_MOD_AS_REM!>%<!> 1

    ModAndRemExtension % 1

    ModExtension <!DEPRECATED_BINARY_MOD_AS_REM!>%<!> 1

    ModMemberAndRemExtension % 1

    OldMod.mod(1)
    ModExtension.mod(1)
}