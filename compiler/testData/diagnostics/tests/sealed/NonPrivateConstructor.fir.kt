sealed class Sealed protected constructor(val x: Int) {
    object FIRST : Sealed()

    public constructor(): this(42)

    constructor(y: Int, z: Int): this(y + z)
}
