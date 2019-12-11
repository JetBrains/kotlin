enum class E public constructor(val x: Int) {
    FIRST();

    internal constructor(): this(42)

    constructor(y: Int, z: Int): this(y + z)
}
