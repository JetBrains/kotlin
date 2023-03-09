// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

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
    constructor(z: Any)

    constructor(y: Int): this()
}
