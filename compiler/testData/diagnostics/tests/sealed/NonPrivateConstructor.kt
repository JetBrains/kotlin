sealed class Sealed <!NON_PRIVATE_CONSTRUCTOR_IN_SEALED!>protected<!> constructor(val x: Int) {
    object FIRST : Sealed()

    <!NON_PRIVATE_CONSTRUCTOR_IN_SEALED!>public<!> constructor(): this(42)

    constructor(y: Int, z: Int): this(y + z)
}
