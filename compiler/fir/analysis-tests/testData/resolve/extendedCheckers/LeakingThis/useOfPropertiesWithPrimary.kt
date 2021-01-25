// !DIAGNOSTICS: -UNUSED_PARAMETER
class A(val w: Int) {
    val x: Int

    val v = 1
    val y = 1
    val uninitialized: Int

    init {
        x + y + v + uninitialized + w
    }

    init {
        x + w  // not a leaking this due init block
        x = 1
        uninitialized = 1
    }
}
