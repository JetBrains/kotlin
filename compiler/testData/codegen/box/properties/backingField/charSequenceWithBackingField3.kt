// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND: NATIVE
// ^KT-83269
// LANGUAGE: +ExplicitBackingFields


class Base {
    val x: CharSequence
        @Suppress("WRONG_MODIFIER_TARGET")
        internal field: String = "OK"

}
val s: String get() = Base().x
fun box(): String {
    return s
}
