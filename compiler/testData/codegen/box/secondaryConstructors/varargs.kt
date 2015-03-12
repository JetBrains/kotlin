fun join(x: Array<out String>): String {
    var result = ""
    for (i in x) {
        result += i
        result += "#"
    }

    return result
}

open class B {
    val parentProp: String
    constructor(vararg x: String) {
        parentProp = join(x)
    }
}

class A : B {
    val prop: String
    constructor(vararg x: String): super("0", *x, "4") {
        prop = join(x)
    }
}

fun box(): String {
    val a1 = A("1", "2", "3")
    if (a1.prop != "1#2#3#") return "fail1: ${a1.prop}"
    if (a1.parentProp != "0#1#2#3#4#") return "fail2: ${a1.parentProp}"

    return "OK"
}
