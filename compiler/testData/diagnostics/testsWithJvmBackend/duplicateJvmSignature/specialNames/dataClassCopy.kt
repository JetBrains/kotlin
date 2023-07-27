// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

<!CONFLICTING_JVM_DECLARATIONS!>data class C(val c: Int) {
    <!CONFLICTING_JVM_DECLARATIONS!>fun `copy$default`(c: C, x: Int, m: Int, mh: Any) = C(this.c)<!>
}<!>
