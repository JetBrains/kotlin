// !DIAGNOSTICS: -UNUSED_PARAMETER

object OldMod {
    <!DEPRECATED_BINARY_MOD!>operator<!> fun mod(x: Int) {}
}

object RemExtension
operator fun RemExtension.rem(x: Int) {}

fun foo() {
    OldMod <!DEPRECATED_BINARY_MOD_AS_REM!>%<!> 123
}