fun <<!CYCLIC_GENERIC_UPPER_BOUND!>T : F?<!>, F : T?> foo1() {}

fun <T : F?, <!CYCLIC_GENERIC_UPPER_BOUND!>F : E<!>, E : F?> foo2() {}

fun <<!CYCLIC_GENERIC_UPPER_BOUND!>T<!>, F> foo3() where T : F?, F : T {}

fun <T, <!CYCLIC_GENERIC_UPPER_BOUND!>F<!>, E> foo4() where T : F?, F : E, E : F? {}