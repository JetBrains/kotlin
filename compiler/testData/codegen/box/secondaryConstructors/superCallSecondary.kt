var sideEffects: String = ""

abstract class B {
    val parentProp: String
    init {
        sideEffects += "minus-one#"
    }
    protected constructor(arg: Int) {
        parentProp = (arg).toString()
        sideEffects += "0.5#"
    }
    protected constructor(arg1: Int, arg2: Int) {
        parentProp = (arg1 + arg2).toString()
        sideEffects += "0.7#"
    }
    init {
        sideEffects += "zero#"
    }
}

class A : B {
    var prop: String = ""
    init {
        sideEffects += prop + "first"
    }

    constructor(x1: Int, x2: Int): super(x1, x2) {
        prop = x1.toString()
        sideEffects += "#third"
    }

    init {
        sideEffects += prop + "#second"
    }

    constructor(x: Int): super(3 + x) {
        prop += "${x}#int"
        sideEffects += "#fourth"
    }

    constructor(): this(7) {
        sideEffects += "#fifth"
    }
}

fun box(): String {
    val a1 = A(5, 10)
    if (a1.prop != "5") return "fail0: ${a1.prop}"
    if (a1.parentProp != "15") return "fail1: ${a1.parentProp}"
    if (sideEffects != "minus-one#zero#0.7#first#second#third") return "fail2: ${sideEffects}"

    sideEffects = ""
    val a2 = A(123)
    if (a2.prop != "123#int") return "fail3: ${a2.prop}"
    if (a2.parentProp != "126") return "fail4: ${a2.parentProp}"
    if (sideEffects != "minus-one#zero#0.5#first#second#fourth") return "fail5: ${sideEffects}"

    sideEffects = ""
    val a3 = A()
    if (a3.prop != "7#int") return "fail6: ${a3.prop}"
    if (a3.parentProp != "10") return "fail7: ${a3.parentProp}"
    if (sideEffects != "minus-one#zero#0.5#first#second#fourth#fifth") return "fail8: ${sideEffects}"
    return "OK"
}
