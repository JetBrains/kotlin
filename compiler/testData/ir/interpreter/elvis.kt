@CompileTimeCalculation
fun getLenght(value: String?): Int = value?.length ?: -1

const val a1 = <!EVALUATED: `5`!>getLenght("Elvis")<!>
const val a2 = <!EVALUATED: `-1`!>getLenght(null)<!>
