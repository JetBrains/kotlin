// !WITH_NEW_INFERENCE
fun foo(x: String?, y: String?, z: String?, w: String?) {
    if (x != null && y != null && (x == z || y == z))
        <!DEBUG_INFO_SMARTCAST!>z<!>.length
    else
        z<!UNSAFE_CALL!>.<!>length
    if (x != null || y != null || (<!DEBUG_INFO_CONSTANT!>x<!> != z && <!DEBUG_INFO_CONSTANT!>y<!> != z))
        z<!UNSAFE_CALL!>.<!>length
    else
        <!DEBUG_INFO_CONSTANT!>z<!><!UNSAFE_CALL!>.<!>length
    if (x == null || y == null || (x != z && y != z))
        z<!UNSAFE_CALL!>.<!>length
    else
        <!DEBUG_INFO_SMARTCAST!>z<!>.length
    if (x != null && y == x && z == y && w == z)
        <!DEBUG_INFO_SMARTCAST!>w<!>.length
    else
        w<!UNSAFE_CALL!>.<!>length
    if ((x != null && y == x) || (z != null && y == z))
        <!DEBUG_INFO_SMARTCAST!>y<!>.length
    else
        y<!UNSAFE_CALL!>.<!>length
}