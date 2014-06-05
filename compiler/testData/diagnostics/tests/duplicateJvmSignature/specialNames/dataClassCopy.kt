// !DIAGNOSTICS: -UNUSED_PARAMETER

data class <!CONFLICTING_JVM_DECLARATIONS!>C(val c: Int)<!> {
    <!CONFLICTING_JVM_DECLARATIONS!>fun `copy$default`(c: C, x: Int, m: Int)<!> = C(this.c)
}