// ISSUE: KT-64634

class Outer(val offset: Int) {
    var log = ""

    fun argument(name: String, value: Int): Int {
        log += name
        return offset + value
    }

    inner class Inner(val first: Int, val second: Int, val third: Int = 30) {
        init {
            log += "init;"
        }

        constructor(value: Double) : this(
            second = argument("second;", 2),
            first = argument("first;", value.toInt()),
        ) {
            log += "secondary;"
        }

        constructor() : this(1.0) {
            log += "chain;"
        }

        fun result() = offset + first + second + third
    }
}

fun box(): String {
    val firstOuter = Outer(10)
    val direct = firstOuter.Inner(1.0)
    if (direct.first != 11) return "fail direct first: ${direct.first}"
    if (direct.second != 12) return "fail direct second: ${direct.second}"
    if (direct.third != 30) return "fail direct third: ${direct.third}"
    if (direct.result() != 63) return "fail direct: ${direct.result()}"
    if (firstOuter.log != "second;first;init;secondary;") return "fail direct log: ${firstOuter.log}"

    val secondOuter = Outer(20)
    val chained = secondOuter.Inner()
    if (chained.first != 21) return "fail chained first: ${chained.first}"
    if (chained.second != 22) return "fail chained second: ${chained.second}"
    if (chained.third != 30) return "fail chained third: ${chained.third}"
    if (chained.result() != 93) return "fail chained: ${chained.result()}"
    if (secondOuter.log != "second;first;init;secondary;chain;") return "fail chained log: ${secondOuter.log}"
    return "OK"
}

// CHECK_BYTECODE_TEXT
// 1 PUTFIELD Outer\$Inner\.this\$0 : LOuter;
