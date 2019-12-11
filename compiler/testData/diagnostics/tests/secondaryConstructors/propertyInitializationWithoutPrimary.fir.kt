// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    val x: Int
    var y: Int
    val z: Int
    val v = -1

    val uninitialized: Int
    val overinitialized: Int

    constructor() {
        x = 1
        y = 2

        overinitialized = 3
        uninitialized = 4
    }

    constructor(a: Int): super() {
        x = 5
        y = 6
    }

    constructor(x: String): this() {
        y = 7
        uninitialized = 8
    }

    //anonymous
    init {
        z = 9
        overinitialized = 10
    }

    // anonymous
    init {
        y = 12
    }
}
