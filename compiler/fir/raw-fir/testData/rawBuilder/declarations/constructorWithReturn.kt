open class Base(x: Int)

class Owner : Base {
    constructor(x: Int = 1) : super(x + 1) {
        if (x < 0) return
        bar(x)
    }

    fun bar(i: Int) {}
}
