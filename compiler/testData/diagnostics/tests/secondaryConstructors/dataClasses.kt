// !DIAGNOSTICS: -UNUSED_PARAMETER
data class A1(val x: String) {
    constructor(): this("")
}

data class A2(val y: String, val z: Int) {
    constructor(x: String): this(x, 0)
}

data class <!DATA_CLASS_WITHOUT_PARAMETERS, PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS!>A3<!> {
    constructor()
}

data class A4 internal constructor<!DATA_CLASS_WITHOUT_PARAMETERS!>()<!>
