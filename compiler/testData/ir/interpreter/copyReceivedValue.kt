@CompileTimeCalculation
fun Int.minusOne(): Int {
    var value = this
    value = value - 1
    return this
}

const val a = <!EVALUATED: `5`!>5.minusOne()<!>
