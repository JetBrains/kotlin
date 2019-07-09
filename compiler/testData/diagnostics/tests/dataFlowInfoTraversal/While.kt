fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null
    while (x == null) {
        bar(<!DEBUG_INFO_CONSTANT, TYPE_MISMATCH!>x<!>)
    }
    bar(<!DEBUG_INFO_SMARTCAST!>x<!>)
    
    val y: Int? = null
    while (y != null) {
        bar(<!DEBUG_INFO_SMARTCAST!>y<!>)
    }
    bar(<!DEBUG_INFO_CONSTANT, TYPE_MISMATCH!>y<!>)
    
    val z: Int? = null
    while (z == null) {
        bar(<!DEBUG_INFO_CONSTANT, TYPE_MISMATCH!>z<!>)
        break
    }
    bar(<!TYPE_MISMATCH!>z<!>)
}
