open class Base {
    open val x: CharSequence = "BASE"
         // field = "BASE"
}

class Ok : Base() {
    override val x: CharSequence
        internal field: String = "OK"
}

fun box(): String {
    return Ok().x
}
