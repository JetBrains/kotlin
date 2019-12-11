// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    val x: Int
    val useUnitialized = x + // reported on each secondary constructor
                         y +
                         v
    var y: Int
    val v = -1

    val useInitialized = useUnitialized + v

    val uninitialized: Int

    constructor() {
        x = 1
        y = 2

        x + y + v + uninitialized

        uninitialized = 3

        x + y + v + uninitialized
    }

    constructor(a: Int): super() {
        x + y + v + uninitialized
        x = 4
        y = 5

        x + y + v + uninitialized
    }

    constructor(x: String): this() {
        x + y + v + uninitialized
    }

    //anonymous
    init {
        y
    }

    // anonymous
    init {
        y = 9
    }
}
