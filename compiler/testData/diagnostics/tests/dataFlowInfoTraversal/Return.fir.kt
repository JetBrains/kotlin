// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
fun bar(x: Int): Int = x + 1

fun foo(): Int {
    val x: Int? = null

    <!INAPPLICABLE_CANDIDATE!>bar<!>(x)
    if (x != null) return x
    
    val y: Int? = null
    if (y == null) return if (y != null) y else y
    
    val z: Int? = null
    if (z != null) return if (z == null) z else z
    
    return <!RETURN_TYPE_MISMATCH!>z<!>
}
