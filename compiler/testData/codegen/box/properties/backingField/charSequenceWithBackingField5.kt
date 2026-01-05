// IGNORE_BACKEND_K1: ANY
// ^KT-83269
// LANGUAGE: +ExplicitBackingFields


open class Base {
    open val x: CharSequence = "BASE"
         // field = "BASE"
}

class Ok : Base() {
    override val x: CharSequence
        @Suppress("WRONG_MODIFIER_TARGET", "NON_FINAL_PROPERTY_WITH_EXPLICIT_BACKING_FIELD")
        internal field: String = "OK"
}

fun box(): String {
    return Ok().x
}
