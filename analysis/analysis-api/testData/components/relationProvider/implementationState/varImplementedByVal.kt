interface Base {
    var x: Int
}

interface Provider : Base {
    override val x: Int get() = 42
}

abstract class <caret>Child : Provider
