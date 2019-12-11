// !LANGUAGE: -ProhibitOperatorMod
// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

object OldMod {
    operator fun mod(x: Int) {}
}

object RemExtension
operator fun RemExtension.rem(x: Int) {}

fun foo() {
    OldMod <!INAPPLICABLE_CANDIDATE!>%<!> 123
}