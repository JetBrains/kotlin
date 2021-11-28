@CompileTimeCalculation
fun tryCatch(integer: Int): String {
    var str = ""
    try {
        str += "Start dividing\n"
        val a = 10 / integer
        str += "Without exception\n"
    } catch (e: ArithmeticException) {
        str += "Exception\n"
    } finally {
        str += "Finally\n"
    }
    return str
}

const val a1 = <!EVALUATED: `Start dividing
Exception
Finally
`!>tryCatch(0)<!>
const val a2 = <!EVALUATED: `Start dividing
Without exception
Finally
`!>tryCatch(1)<!>
const val a3 = <!EVALUATED: `Start dividing
Without exception
Finally
`!>tryCatch(100)<!>
