var sideEffects: String = ""

abstract class B protected(val arg: Int) {
    val parentProp: String
    init {
        sideEffects += "zero#"
        parentProp = arg.toString()
    }
}

class A(x: Boolean) : B(if (x) 1 else 2) {
    var prop: String = ""
    init {
        sideEffects += prop + "first"
    }

    constructor(x: String): this(x == "abc") {
        prop = x
        sideEffects += "#third"
    }

    init {
        sideEffects += prop + "#second"
    }

    constructor(x: Int): this(x < 0) {
        prop += "${x}#int"
        sideEffects += "#fourth"
    }
}

fun box(): String {
    val a1 = A("abc")
    if (a1.prop != "abc") return "fail0: ${a1.prop}"
    if (a1.parentProp != "1") return "fail1: ${a1.parentProp}"
    if (a1.arg != 1) return "fail1': ${a1.arg}"
    if (sideEffects != "zero#first#second#third") return "fail2: ${sideEffects}"

    sideEffects = ""
    val a2 = A(123)
    if (a2.prop != "123#int") return "fail3: ${a2.prop}"
    if (a2.parentProp != "2") return "fail4: ${a2.parentProp}"
    if (a2.arg != 2) return "fail5': ${a2.arg}"
    if (sideEffects != "zero#first#second#fourth") return "fail6: ${sideEffects}"

    sideEffects = ""
    val a3 = A(false)
    if (a3.prop != "") return "fail7: ${a3.prop}"
    if (a3.parentProp != "2") return "fail8: ${a3.parentProp}"
    if (a3.arg != 2) return "fail9': ${a3.arg}"
    if (sideEffects != "zero#first#second") return "fail10: ${sideEffects}"
    return "OK"
}
