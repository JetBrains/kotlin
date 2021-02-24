class Outer {
    val outerProp: String
    constructor(x: String) {
        outerProp = x
    }

    var sideEffects = ""

    abstract inner class A1 {
        var parentProp: String = ""
        init {
            sideEffects += outerProp + "#" + parentProp + "first"
        }

        protected constructor(x: String) {
            parentProp = x + "#${outerProp}"
            sideEffects += "#second#"
        }

        init {
            sideEffects += parentProp + "#third"
        }

        protected constructor(x: Int): this(x.toString() + "#" + outerProp) {
            parentProp += "#int"
            sideEffects += "fourth#"
        }
    }

    inner class A2 : A1 {
        var prop: String = ""
        init {
            sideEffects += outerProp + "#" + prop + "fifth"
        }

        constructor(x: String): super(x + "#" + outerProp) {
            prop = x + "#$outerProp"
            sideEffects += "#sixth"
        }

        init {
            sideEffects += prop + "#seventh"
        }

        constructor(x: Int): super(x + 1) {
            prop += "$x#$outerProp#int"
            sideEffects += "#eighth"
        }
    }
}

fun box(): String {
    val outer1 = Outer("propValue1")
    val a1 = outer1.A2("abc")
    if (a1.parentProp != "abc#propValue1#propValue1") return "fail1: ${a1.parentProp}"
    if (a1.prop != "abc#propValue1") return "fail2: ${a1.prop}"
    if (outer1.sideEffects != "propValue1#first#third#second#propValue1#fifth#seventh#sixth") return "fail1-sideEffects: ${outer1.sideEffects}"

    val outer2 = Outer("propValue2")
    val a2 = outer2.A2(123)
    if (a2.parentProp != "124#propValue2#propValue2#int") return "fail3: ${a2.parentProp}"
    if (a2.prop != "123#propValue2#int") return "fail4: ${a2.prop}"
    if (outer2.sideEffects != "propValue2#first#third#second#fourth#propValue2#fifth#seventh#eighth") return "fail2-sideEffects: ${outer2.sideEffects}"

    return "OK"
}
