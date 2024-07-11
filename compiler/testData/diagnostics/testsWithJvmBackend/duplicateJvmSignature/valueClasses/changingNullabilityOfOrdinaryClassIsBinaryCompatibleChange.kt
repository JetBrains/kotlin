// FIR_IDENTICAL
// LANGUAGE: +InlineClasses
// DIAGNOSTICS: -UNUSED_PARAMETER, -INLINE_CLASS_DEPRECATED

inline class IC(val i: Int)

<!CONFLICTING_JVM_DECLARATIONS!>fun foo(a: Any, ic: IC) {}<!>
<!CONFLICTING_JVM_DECLARATIONS!>fun foo(a: Any?, ic: IC) {}<!>
