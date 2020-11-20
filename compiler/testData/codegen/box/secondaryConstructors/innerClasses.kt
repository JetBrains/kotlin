class Outer {
    val outerProp: String
    constructor(x: String) {
        outerProp = x
    }

    var sideEffects = ""

    inner class A1() {
        var prop: String = ""
        init {
            sideEffects += outerProp + "#" + prop + "first"
        }

        constructor(x: String): this() {
            prop = x + "#${outerProp}"
            sideEffects += "#third"
        }

        init {
            sideEffects += prop + "#second"
        }

        constructor(x: Int): this(x.toString() + "#" + outerProp) {
            prop += "#int"
            sideEffects += "#fourth"
        }
    }

    inner class A2 {
        var prop: String = ""
        init {
            sideEffects += outerProp + "#" + prop + "first"
        }

        constructor(x: String) {
            prop = x + "#$outerProp"
            sideEffects += "#third"
        }

        init {
            sideEffects += prop + "#second"
        }

        constructor(x: Int) {
            prop += "$x#$outerProp#int"
            sideEffects += "#fourth"
        }
    }
}

fun box(): String {
    val outer1 = Outer("propValue1")
    val a1 = outer1.A1("abc")
    if (a1.prop != "abc#propValue1") return "fail1: ${a1.prop}"
    if (outer1.sideEffects != "propValue1#first#second#third") return "fail1-sideEffects: ${outer1.sideEffects}"

    val outer2 = Outer("propValue2")
    val a2 = outer2.A1(123)
    if (a2.prop != "123#propValue2#propValue2#int") return "fail2: ${a2.prop}"
    if (outer2.sideEffects != "propValue2#first#second#third#fourth") return "fail2-sideEffects: ${outer2.sideEffects}"

    val outer3 = Outer("propValue3")
    val a3 = outer3.A1()
    if (a3.prop != "") return "fail2: ${a3.prop}"
    if (outer3.sideEffects != "propValue3#first#second") return "fail3-sideEffects: ${outer3.sideEffects}"

    val outer4 = Outer("propValue4")
    val a4 = outer4.A2("abc")
    if (a4.prop != "abc#propValue4") return "fail4: ${a4.prop}"
    if (outer4.sideEffects != "propValue4#first#second#third") return "fail4-sideEffects: ${outer4.sideEffects}"

    val outer5 = Outer("propValue5")
    val a5 = outer5.A2(123)
    if (a5.prop != "123#propValue5#int") return "fail5: ${a5.prop}"
    if (outer5.sideEffects != "propValue5#first#second#fourth") return "fail5-sideEffects: ${outer5.sideEffects}"

    return "OK"
}
