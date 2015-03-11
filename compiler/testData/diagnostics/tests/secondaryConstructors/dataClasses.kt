// !DIAGNOSTICS: -UNUSED_PARAMETER
data class A1(val x: String) {
    constructor(): this("") {}
}

data class A2() {
    constructor(x: String): this() {}
}

data class <!PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS!>A3<!> {
    constructor() {}
}
