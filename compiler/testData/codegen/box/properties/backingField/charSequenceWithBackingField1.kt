// IGNORE_BACKEND_K1: ANY
// ^KT-83269
// LANGUAGE: +ExplicitBackingFields

class Base {
    val x: CharSequence
        @Suppress("WRONG_MODIFIER_TARGET")
        internal field: String = "OK"
}

fun box(): String {
    return Base().x
}
