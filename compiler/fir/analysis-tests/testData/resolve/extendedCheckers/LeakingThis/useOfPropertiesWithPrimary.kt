// !DIAGNOSTICS: -UNUSED_PARAMETER
class A(val w: Int) {
    val x: Int
    // anonymous
    init {
        x + w  // not a leaking this due init block
        x = 1
    }
}
