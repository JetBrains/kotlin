// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

<!CONFLICTING_JVM_DECLARATIONS!>fun foo(vararg x: Int) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(x: IntArray) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun foo(vararg x: Int?) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(x: Array<Int>) {}<!>

<!CONFLICTING_JVM_DECLARATIONS!>fun foo(vararg nn: Number) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(nn: Array<out Number>) {}<!>
