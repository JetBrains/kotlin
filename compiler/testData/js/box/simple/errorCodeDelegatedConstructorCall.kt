// ISSUE: KT-64548

open external class Light {
    constructor(hex: Number = definedExternally, intensity: Number = definedExternally)
    constructor(hex: String = definedExternally, intensity: Number = definedExternally)
}

open external class HemisphereLight : Light {
    constructor(intensity: Number = definedExternally)
}

fun box() = "OK"
