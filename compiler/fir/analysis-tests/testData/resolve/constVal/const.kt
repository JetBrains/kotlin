
const val a = "something"
<!MUST_BE_INITIALIZED!><!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val b<!>
<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val c = null
<!TYPE_CANT_BE_USED_FOR_CONST_VAL!>const<!> val d = ForConst
const val e = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>ForConst.one()<!>
const val f = ((1 + 2) * 3) / 4 % 5 - 1
const val g = "string $f"
const val h = "string" + g
const val i = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>ForConst.one() + "one"<!>
const val j = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>4 * ForConst.two()<!>
val k = 3 - ForConst.two()
const val l = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>k<!>

class ForConst{
    companion object {
        fun one(): String = "1"
        fun two(): Int = 2
    }
}