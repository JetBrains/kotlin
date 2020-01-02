// Works already in M11

fun test(c : Class<*>) {
    val sc = c as Class<String>
    // No ambiguous overload
    c.getAnnotations();
    sc.getAnnotations();
}
