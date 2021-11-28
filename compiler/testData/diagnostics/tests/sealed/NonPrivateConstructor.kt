sealed class Sealed protected constructor(val x: Int) {
    object FIRST : Sealed()

    <!NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED!>public<!> constructor(): this(42)

    constructor(y: Int, z: Int): this(y + z)
}
