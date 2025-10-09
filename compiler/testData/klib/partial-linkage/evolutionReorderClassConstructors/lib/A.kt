open class X(open val y: String) {
    constructor(x: String, y: String): this(x+y)
    constructor(x: String, y: String, z: String): this(x, y+z)
}

