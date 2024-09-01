private <!EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT!>var Int.readOnlyWrapper: CharSequence?<!> get() = null
private <!EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT!>var Int.mutableWrapper: CharSequence?<!> get() = null

fun main(x: Int) {
    val x = if (x > 1) x::readOnlyWrapper else x::mutableWrapper

    x.get()
}
