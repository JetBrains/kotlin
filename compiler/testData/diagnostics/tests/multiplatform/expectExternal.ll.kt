// LL_FIR_DIVERGENCE
// WRONG_MODIFIER_TARGET not reported in LL and reported in CLI tests in common module,
// because it is JVM-only checker, and in CLI common module analyzed additionally against JVM target.
// LL_FIR_DIVERGENCE
// MODULE: m1-common
// FILE: common.kt

expect <!EXPECTED_EXTERNAL_DECLARATION!>external<!> fun foo()
expect fun bar()

expect <!EXPECTED_EXTERNAL_DECLARATION!>external<!> var prop: String

expect var getAndSet: String
    <!EXPECTED_EXTERNAL_DECLARATION!>external<!> get
    <!EXPECTED_EXTERNAL_DECLARATION!>external<!> set

<!EXPECTED_EXTERNAL_DECLARATION!>external<!> expect val explicitGetter: String
    <!EXPECTED_EXTERNAL_DECLARATION!>external<!> get

expect <!EXPECTED_EXTERNAL_DECLARATION!>external<!> class A {
    <!EXPECTED_EXTERNAL_DECLARATION!>external<!> fun foo()
    fun bar()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual external fun foo()
actual external fun bar()

actual <!WRONG_MODIFIER_TARGET!>external<!> var prop: String

actual var getAndSet: String
    external get
    external set

actual <!WRONG_MODIFIER_TARGET!>external<!> val explicitGetter: String
    external get

actual class A {
    actual external fun foo()
    actual external fun bar()
}
