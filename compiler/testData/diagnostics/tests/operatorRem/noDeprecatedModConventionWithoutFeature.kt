// !LANGUAGE: -OperatorRem
// !DIAGNOSTICS: -UNUSED_PARAMETER

object ModAndRem {
    operator fun mod(x: Int) {}
    <!UNSUPPORTED_FEATURE!>operator<!> fun rem(x: Int) {}
}

object OldMod {
    operator fun mod(x: Int) {}
}

object ModAndRemExtension
operator fun ModAndRemExtension.mod(x: Int) {}
<!UNSUPPORTED_FEATURE!>operator<!> fun ModAndRemExtension.rem(x: Int) {}

object ModExtension
operator fun ModExtension.mod(x: Int) {}

object ModMemberAndRemExtension {
    operator fun mod(x: Int) {}
}

<!UNSUPPORTED_FEATURE!>operator<!> fun ModMemberAndRemExtension.rem(x: Int) {}

fun foo() {
    ModAndRem % 1
    OldMod % 1

    ModAndRemExtension % 1

    ModExtension % 1

    ModMemberAndRemExtension % 1

    OldMod.mod(1)
    ModExtension.mod(1)
}