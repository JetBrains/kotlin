// !LANGUAGE: -ProhibitOperatorMod
// !DIAGNOSTICS: -UNUSED_PARAMETER

object A {
    @Deprecated("Use mod instead", ReplaceWith("mod"), DeprecationLevel.HIDDEN)
    operator fun rem(x: Int) = 0

    operator fun mod(x: Int) = ""
}

fun test() {
    <!INAPPLICABLE_CANDIDATE!>takeString<!>(A % 123)
}

fun takeString(s: String) {}