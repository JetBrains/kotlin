// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    <!CONFLICTING_JVM_DECLARATIONS!>fun `a$default`(c: C, x: Int, m: Int, mh: Any) {}<!>
    <!CONFLICTING_JVM_DECLARATIONS!>fun a(x: Int = 1) {}<!>
}
