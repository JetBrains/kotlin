// !DIAGNOSTICS: -UNUSED_PARAMETER
data class A1(val x: String) {
    constructor(): this("")
}

data class A2(val y: String, val z: Int) {
    constructor(x: String): this(x, 0)
}

data <!DATA_CLASS_WITHOUT_PARAMETERS!>class A3<!> {
    constructor()
}

data class A4 <!DATA_CLASS_WITHOUT_PARAMETERS!>internal constructor()<!>
