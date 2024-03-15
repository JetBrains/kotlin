// FIR_IDENTICAL
// !LANGUAGE: +InlineClasses, -JvmInlineValueClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

inline class IC(val i: Int)

<!CONFLICTING_JVM_DECLARATIONS!>fun foo(a: Any, ic: IC) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(a: Any?, ic: IC) {}<!>
