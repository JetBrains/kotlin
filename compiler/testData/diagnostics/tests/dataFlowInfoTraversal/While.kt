fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null
    while (x == null) {
        bar(<!TYPE_MISMATCH, DEBUG_INFO_CONSTANT!>x<!>)
    }
    bar(<!DEBUG_INFO_SMARTCAST!>x<!>)
    
    val y: Int? = null
    while (y != null) {
        bar(<!DEBUG_INFO_SMARTCAST!>y<!>)
    }
    bar(<!TYPE_MISMATCH, DEBUG_INFO_CONSTANT!>y<!>)
    
    val z: Int? = null
    while (z == null) {
        bar(<!TYPE_MISMATCH, DEBUG_INFO_CONSTANT!>z<!>)
        break
    }
    bar(<!TYPE_MISMATCH!>z<!>)
}
