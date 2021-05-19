@CompileTimeCalculation
fun Int.minusOne(): Int {
    var value = this
    value = value - 1
    return this
}

const val a = 5.<!EVALUATED: `5`!>minusOne()<!>
