// !LANGUAGE: +IntrinsicConstEvaluation

const val flag = true
const val value = 10

const val condition = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>if (flag) "True" else "Error"<!>
const val withWhen = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>when (flag) {
    true -> "True"
    else -> "Error"
}<!>
const val withWhen2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>when {
    flag == true -> "True"
    else -> "Error"
}<!>
const val withWhen3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>when(value) {
    10 -> "1"
    100 -> "2"
    else -> "3"
}<!>
const val multibranchIf = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>if (value == 100) 1 else if (value == 1000) 2 else 3<!>

val nonConstFlag = true
const val errorConstIf = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>if (nonConstFlag) 1 else 2<!>
const val errorBranch = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>if (flag) nonConstFlag else false<!>
