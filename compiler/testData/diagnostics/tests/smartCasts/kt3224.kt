// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
// Works already in M11

fun test(c : Class<*>) {
    val sc = c <!UNCHECKED_CAST!>as Class<String><!>
    // No ambiguous overload
    c.getAnnotations();
    sc.getAnnotations();
}
