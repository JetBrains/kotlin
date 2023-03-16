// !LANGUAGE: +IntrinsicConstEvaluation

const val flag = true
const val value = 10

const val condition = if (flag) "True" else "Error"
const val withWhen = when (flag) {
    true -> "True"
    else -> "Error"
}
const val withWhen2 = when {
    flag == true -> "True"
    else -> "Error"
}
const val withWhen3 = when(value) {
    10 -> "1"
    100 -> "2"
    else -> "3"
}
const val multibranchIf = if (value == 100) 1 else if (value == 1000) 2 else 3

val nonConstFlag = true
const val errorConstIf = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>if (nonConstFlag) 1 else 2<!>
const val errorBranch = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>if (flag) nonConstFlag else false<!>
