// FIR_IDENTICAL

object O : Code(0)

open class Code(val x: Int) {
    override fun toString() = "$x"
}

class A {
    companion object: Code(0)
}

const val toString1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>O.toString()<!>
const val toString2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>A.toString()<!>
const val plusString1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"string" + O<!>
const val plusString2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"string" + A<!>
const val stringConcat1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"$O"<!>
const val stringConcat2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"$A"<!>
