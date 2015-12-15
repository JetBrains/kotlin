// Works already in M11

fun test(c : Class<*>) {
    val sc = <!UNCHECKED_CAST!>c as Class<String><!>
    // No ambiguous overload
    c.getAnnotations();
    sc.getAnnotations();
}
