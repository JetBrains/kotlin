// !DIAGNOSTICS: -UNUSED_PARAMETER
data class A1(val x: String) {
    constructor(): this("")
}

data class A2(val y: String, val z: Int) {
    constructor(x: String): this(x, 0)
}

data <!PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS!>class A3<!> {
    constructor()
}

data class A4 <!DATA_CLASS_WITHOUT_PARAMETERS!>internal constructor()<!>
