open class B(x: Int)
class A : B {
    val x: Int
    var y: Int
    val z: Int
    val v = -1

    constructor(): super(11) {
        x = 1
        y = 2
    }
    constructor(a: Int, b: Int = 3): super(b) {
        x = a
        y = x
    }

    // anonymous
    init {
        z = 8
    }

    constructor(a: String, b: Int = 4): this() {
        y = 5
    }
    constructor(a: Double, b: Int = 6): this(a.toInt()) {
        y = 7
    }

    // anonymous
    init {
        y = 9
    }
}
