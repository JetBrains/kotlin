// !DIAGNOSTICS: -UNUSED_PARAMETER

data class C(val c: Int) {
    <!CONFLICTING_PLATFORM_DECLARATIONS!>fun `copy$default`(c: C, x: Int, m: Int)<!> = 1
}