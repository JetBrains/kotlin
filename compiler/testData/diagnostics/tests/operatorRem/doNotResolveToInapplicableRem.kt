// !LANGUAGE: -ProhibitOperatorMod
// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

object OldMod {
    <!DEPRECATED_BINARY_MOD!>operator<!> fun mod(x: Int) {}
}

object RemExtension
operator fun RemExtension.rem(x: Int) {}

fun foo() {
    OldMod <!OI;DEPRECATED_BINARY_MOD_AS_REM!>%<!> 123
}