// RUN_PIPELINE_TILL: FRONTEND
const val simple = "O${'K'} ${1.toLong() + 2.0}"
const val withInnerConcatenation = "1 ${"2 ${3} ${4} 5"} 6"

object A
object B {
    override fun toString(): String = "B"
}

const val printA = "A: $<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>A<!>"
const val printB = "B: $<!CONST_VAL_WITH_NON_CONST_INITIALIZER!>B<!>"

const val withNull = "1 ${null}"
const val withNullPlus = "1" + <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>null<!>

val nonConst = 0
const val withNonConst = "A $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>nonConst<!> B"

/* GENERATED_FIR_TAGS: additiveExpression, const, functionDeclaration, integerLiteral, nullableType, objectDeclaration,
override, propertyDeclaration, stringLiteral */
