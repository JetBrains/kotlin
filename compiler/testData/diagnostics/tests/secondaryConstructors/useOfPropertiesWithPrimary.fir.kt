// !DIAGNOSTICS: -UNUSED_PARAMETER
class A(val w: Int) {
    val x: Int
    val useUnitialized = x +
                         y +
                         v
    var y: Int
    val v = -1
    val useInitialized = useUnitialized + v + w

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val uninitialized: Int<!>

    constructor(): this(1) {
        x + y + v + uninitialized + w
    }

    // anonymous
    init {
        x + y + v + uninitialized + w
        x = 1
        x + y + v + uninitialized + w
    }

    // anonymous
    init {
        x + y + v + uninitialized + w
        y = 7
        x + y + v + uninitialized + w
    }
}
