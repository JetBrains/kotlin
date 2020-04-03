var sideEffects: String = ""

class A {
    var prop: String = ""
    init {
        sideEffects += prop + "first"
    }

    constructor(x: String) {
        prop = x
        sideEffects += "#third"
    }

    init {
        sideEffects += prop + "#second"
    }

    constructor(x: Int) {
        prop += "$x#int"
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
    if (sideEffects != "first#second#fourth") return "fail2-sideEffects: ${sideEffects}"

    return "OK"
}
