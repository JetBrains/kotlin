// FIR_DIFFERENCE
// K1 doesn't detect a clash between constructors in one class.
// It doesn't seem correct (KT-64976) because it leads to a clash in the generated JS code. K2 works correctly.
// !DIAGNOSTICS: -OPT_IN_USAGE

@JsExport
class ClassA {
    val x: String

    @JsName("constructorA")
    constructor(y: String) {
        x = "fromString:$y"
    }

    @JsName("constructorOther")
    constructor(y: Int) {
        x = "fromInt:$y"
    }
}

@JsExport
class ClassB {
    val x: String

    @JsName("constructorB")
    constructor(y: String) {
        x = "fromString:$y"
    }

    @JsName("constructorOther")
    constructor(y: Int) {
        x = "fromInt:$y"
    }
}

@JsExport
class ClassC {
    val x: String

    @JsName("constructorC")
    constructor(y: String) {
        x = "fromString:$y"
    }

    @JsName("constructorC")
    constructor(y: Int) {
        x = "fromInt:$y"
    }
}
