open class C(val grandParentProp: String)
fun box(): String {
    var sideEffects: String = ""
    var parentSideEffects: String = ""
    val justForUsageInClosure = 7
    val justForUsageInParentClosure = "parentCaptured"

    abstract class B : C {
        val parentProp: String
        init {
            sideEffects += "minus-one#"
            parentSideEffects += "1"
        }
        protected constructor(arg: Int): super(justForUsageInParentClosure) {
            parentProp = (arg).toString()
            sideEffects += "0.5#"
            parentSideEffects += "#" + justForUsageInParentClosure
        }
        protected constructor(arg1: Int, arg2: Int): super(justForUsageInParentClosure) {
            parentProp = (arg1 + arg2).toString()
            sideEffects += "0.7#"
            parentSideEffects += "#3"
        }
        init {
            sideEffects += "zero#"
            parentSideEffects += "#4"
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

        constructor(x: Int): super(justForUsageInClosure + x) {
            prop += "${x}#int"
            sideEffects += "#fourth"
        }

        constructor(): this(justForUsageInClosure) {
            sideEffects += "#fifth"
        }

        override fun toString() = "$prop#$parentProp#$grandParentProp"
    }

    val a1 = A(5, 10).toString()
    if (a1 != "5#15#parentCaptured") return "fail1: $a1"
    if (sideEffects != "minus-one#zero#0.7#first#second#third") return "fail2: ${sideEffects}"
    if (parentSideEffects != "1#4#3") return "fail3: ${parentSideEffects}"

    sideEffects = ""
    parentSideEffects = ""
    val a2 = A(123).toString()
    if (a2 != "123#int#130#parentCaptured") return "fail1: $a2"
    if (sideEffects != "minus-one#zero#0.5#first#second#fourth") return "fail4: ${sideEffects}"
    if (parentSideEffects != "1#4#parentCaptured") return "fail5: ${parentSideEffects}"

    sideEffects = ""
    parentSideEffects = ""
    val a3 = A().toString()
    if (a3 != "7#int#14#parentCaptured") return "fail6: $a3"
    if (sideEffects != "minus-one#zero#0.5#first#second#fourth#fifth") return "fail7: ${sideEffects}"
    if (parentSideEffects != "1#4#parentCaptured") return "fail8: ${parentSideEffects}"

    return "OK"
}
