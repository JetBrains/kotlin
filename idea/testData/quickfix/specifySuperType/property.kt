// "Specify supertype" "true"
interface X

open class Y {
    open val bar
        get() = 1
}

interface Z {
    val bar
        get() = 2
}

class Test : X, Z, Y() {
    override val bar: Int
        get() = <caret>super.bar
}