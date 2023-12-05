// FILE: Constants.java

public class Constants {
    public static final String FIRST = "1st";
    public static final String SECOND = "2nd";
}

// FILE: const.kt
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
const val m = "123".toString()
const val n = "456".length
val o = "789"
const val p = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>o.toString()<!>
const val q = <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>o.length<!>

class ForConst{
    companion object {
        fun one(): String = "1"
        fun two(): Int = 2
    }
}

private const val MAJOR_BITS = 3
private const val MINOR_BITS = 4
private const val PATCH_BITS = 7
private const val MAJOR_MASK = (1 shl MAJOR_BITS) - 1 // False positive error
private const val MINOR_MASK = (1 shl MINOR_BITS) - 1 // False positive error
private const val PATCH_MASK = (1 shl PATCH_BITS) - 1    // False positive error

private const val stringFromJava = Constants.FIRST + "+" + Constants.SECOND