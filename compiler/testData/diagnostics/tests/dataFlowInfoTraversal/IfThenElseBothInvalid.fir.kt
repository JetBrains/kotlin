// RUN_PIPELINE_TILL: FRONTEND
fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    bar(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    if (x != 2) {
        if (x == null) return
        2+<!SYNTAX!><!>
    }
    else {
        if (<!SENSELESS_COMPARISON!>x == null<!>) return
        2+<!SYNTAX!><!>
    }
    bar(x)
}
