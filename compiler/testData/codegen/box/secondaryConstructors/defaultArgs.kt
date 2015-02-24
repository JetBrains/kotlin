val global = "OK"
class A {
    val prop: String
    constructor(arg1: String = global) {
        prop = arg1
    }
    constructor(arg1: String = global, arg2: Long) {
        prop = "$arg1#$arg2"
    }
    constructor(arg1: String = global, argDouble: Double, arg3: Long = 1L) {
        prop = "$arg1#$argDouble#$arg3"
    }
}

fun box(): String {
    val a1 = A()
    if (a1.prop != "OK") return "fail1: ${a1.prop}"
    val a2 = A("A")
    if (a2.prop != "A") return "fail2: ${a2.prop}"

    val a3 = A(arg2=123)
    if (a3.prop != "OK#123") return "fail3: ${a3.prop}"
    val a4 = A("A", arg2=123)
    if (a4.prop != "A#123") return "fail4: ${a4.prop}"

    val a5 = A(argDouble=23.0)
    if (a5.prop != "OK#23.0#1") return "fail5: ${a5.prop}"
    val a6 = A("A", argDouble=23.0)
    if (a6.prop != "A#23.0#1") return "fail6: ${a6.prop}"
    val a7 = A("A", arg3=2L, argDouble=23.0)
    if (a7.prop != "A#23.0#2") return "fail7: ${a7.prop}"

    return "OK"
}
