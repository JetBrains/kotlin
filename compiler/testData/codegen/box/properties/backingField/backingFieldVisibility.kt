// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND: NATIVE
// ^KT-83269
// LANGUAGE: +ExplicitBackingFields


class A {
    val a: Number
        @Suppress("WRONG_MODIFIER_TARGET")
        private field = 1

    val b: Number
        @Suppress("WRONG_MODIFIER_TARGET")
        internal field = a + 3
}

fun box(): String {
    return if (A().b + 20 == 24) {
        "OK"
    } else {
        "fail: A().b = " + A().b.toString()
    }
}
