const val simple = "O${'K'} ${1.toLong() + 2.0}"
const val withInnerConcatenation = "1 ${"2 ${3} ${4} 5"} 6"

object A
object B {
    override fun toString(): String = "B"
}

const val printA = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"A: $A"<!>
const val printB = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"B: $B"<!>

const val withNull = "1 ${null}"
const val withNullPlus = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"1" + null<!>

val nonConst = 0
const val withNonConst = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>"A $nonConst B"<!>
