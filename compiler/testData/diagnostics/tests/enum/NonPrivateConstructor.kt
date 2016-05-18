enum class E <!NON_PRIVATE_CONSTRUCTOR_IN_ENUM!>public<!> constructor(val x: Int) {
    FIRST();

    <!NON_PRIVATE_CONSTRUCTOR_IN_ENUM!>internal<!> constructor(): this(42)

    constructor(y: Int, z: Int): this(y + z)
}
