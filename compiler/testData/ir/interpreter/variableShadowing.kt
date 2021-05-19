@CompileTimeCalculation
fun returnValueFromA(a: Int, b: Int): Int {
    val a = b
    return a
}

const val num = <!EVALUATED: `2`!>returnValueFromA(1, 2)<!>
