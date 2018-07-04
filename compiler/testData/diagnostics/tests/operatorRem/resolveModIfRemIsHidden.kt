// !LANGUAGE: -ProhibitOperatorMod
// !DIAGNOSTICS: -UNUSED_PARAMETER

object A {
    @Deprecated("Use mod instead", ReplaceWith("mod"), DeprecationLevel.HIDDEN)
    operator fun rem(x: Int) = 0

    <!DEPRECATED_BINARY_MOD!>operator<!> fun mod(x: Int) = ""
}

fun test() {
    takeString(A <!DEPRECATED_BINARY_MOD_AS_REM!>%<!> 123)
}

fun takeString(s: String) {}