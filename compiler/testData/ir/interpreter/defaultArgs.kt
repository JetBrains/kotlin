@CompileTimeCalculation
fun sum(a: Int = 1, b: Int = 2, c: Int = 3) = a + b + c

@CompileTimeCalculation
fun sumBasedOnPrevious(a: Int = 1, b: Int = a * 2, c: Int = b * 2) = a + b + c

const val sum1 = <!EVALUATED: `6`!>sum()<!>
const val sum2 = <!EVALUATED: `1`!>sum(b = -3)<!>
const val sum3 = <!EVALUATED: `3`!>sum(c = 1, a = 1, b = 1)<!>

const val sumBasedOnPrevious1 = <!EVALUATED: `7`!>sumBasedOnPrevious()<!>
const val sumBasedOnPrevious2 = <!EVALUATED: `3`!>sumBasedOnPrevious(b = 1, c = 1)<!>
