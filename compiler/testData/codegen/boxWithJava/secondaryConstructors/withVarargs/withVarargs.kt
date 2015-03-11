fun join(x: Array<out String>): String {
    var result = ""
    for (i in x) {
        result += i
        result += "#"
    }

    return result
}

class A {
    val prop: String
    constructor(vararg x: String) {
        prop = join(x)
    }
}

fun box(): String {
    val a1 = WithVarargs.foo()
    if (a1 != "1#2#3#") return "fail1: ${a1}"

    return "OK"
}
