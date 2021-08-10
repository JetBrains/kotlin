// !LANGUAGE: -ProhibitOperatorMod
// !DIAGNOSTICS: -UNUSED_PARAMETER

object OldMod {
    operator fun mod(x: Int) {}
}

object RemExtension
operator fun RemExtension.rem(x: Int) {}

fun foo() {
    OldMod <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>%<!> 123
}
