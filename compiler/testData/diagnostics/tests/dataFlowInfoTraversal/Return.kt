// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
fun bar(x: Int): Int = x + 1

fun foo(): Int {
    val x: Int? = null

    bar(<!TYPE_MISMATCH!>x<!>)
    if (x != null) return x
    
    val y: Int? = null
    if (y == null) return <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>y<!> != null<!>) y else <!OI;TYPE_MISMATCH, DEBUG_INFO_CONSTANT!>y<!><!>
    
    val z: Int? = null
    if (z != null) return if (<!SENSELESS_COMPARISON!>z == null<!>) z else z
    
    return <!NI;TYPE_MISMATCH, DEBUG_INFO_CONSTANT, TYPE_MISMATCH!>z<!>
}
