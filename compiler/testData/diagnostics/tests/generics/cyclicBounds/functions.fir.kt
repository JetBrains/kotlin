fun <T : F?, F : T?> foo1() {}

fun <T : F?, F : E, E : F?> foo2() {}

fun <T, F> foo3() where T : F?, F : T {}

fun <T, F, E> foo4() where T : F?, F : E, E : F? {}