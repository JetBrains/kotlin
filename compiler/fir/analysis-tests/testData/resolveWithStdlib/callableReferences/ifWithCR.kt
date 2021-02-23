<!MUST_BE_INITIALIZED!>private var Int.readOnlyWrapper: CharSequence?<!> get() = null
<!MUST_BE_INITIALIZED!>private var Int.mutableWrapper: CharSequence?<!> get() = null

fun main(x: Int) {
    val x = if (x > 1) x::readOnlyWrapper else x::mutableWrapper

    x.get()
}
