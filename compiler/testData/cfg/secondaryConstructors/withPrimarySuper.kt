open class B(x: Int)
class A(val w: Char, u: Int = 2) : B(w.toInt() + u) {
    val x: Int
    var y: Int
    val z: Int
    val v = -1

    constructor(): this('a') {
        y = 2
    }

    // anonymous
    init {
        x = w
        z = 8
    }

    constructor(a: Int, b: Int = 3): this(b.toChar()) {
        y = x
    }

    // anonymous
    init {
        y = 9
    }
}
