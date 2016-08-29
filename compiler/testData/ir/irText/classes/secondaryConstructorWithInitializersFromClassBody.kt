open class Base

class TestProperty : Base {
    val x = 0
    constructor()
}

class TestInitBlock : Base {
    val x: Int
    init {
        x = 0
    }
    constructor()
}
