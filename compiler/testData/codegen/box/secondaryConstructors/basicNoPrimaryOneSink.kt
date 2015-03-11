var sideEffects: String = ""

class A {
    var prop: String = ""
    init {
        sideEffects += prop + "first"
    }

    constructor() {}

    constructor(x: String): this() {
        prop = x
        sideEffects += "#third"
    }

    init {
        sideEffects += prop + "#second"
    }

    constructor(x: Int): this(x.toString()) {
        prop += "#int"
        sideEffects += "#fourth"
    }
}

fun box(): String {
    val a1 = A("abc")
    if (a1.prop != "abc") return "fail1: ${a1.prop}"
    if (sideEffects != "first#second#third") return "fail1-sideEffects: ${sideEffects}"

    sideEffects = ""
    val a2 = A(123)
    if (a2.prop != "123#int") return "fail2: ${a2.prop}"
    if (sideEffects != "first#second#third#fourth") return "fail2-sideEffects: ${sideEffects}"

    sideEffects = ""
    val a3 = A()
    if (a3.prop != "") return "fail2: ${a3.prop}"
    if (sideEffects != "first#second") return "fail3-sideEffects: ${sideEffects}"
    return "OK"
}
