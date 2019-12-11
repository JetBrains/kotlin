// !WITH_NEW_INFERENCE
fun foo(x: String?, y: String?, z: String?, w: String?) {
    if (x != null && y != null && (x == z || y == z))
        z.length
    else
        z.<!INAPPLICABLE_CANDIDATE!>length<!>
    if (x != null || y != null || (x != z && y != z))
        z.<!INAPPLICABLE_CANDIDATE!>length<!>
    else
        z.<!INAPPLICABLE_CANDIDATE!>length<!>
    if (x == null || y == null || (x != z && y != z))
        z.<!INAPPLICABLE_CANDIDATE!>length<!>
    else
        z.<!INAPPLICABLE_CANDIDATE!>length<!>
    if (x != null && y == x && z == y && w == z)
        w.length
    else
        w.<!INAPPLICABLE_CANDIDATE!>length<!>
    if ((x != null && y == x) || (z != null && y == z))
        y.length
    else
        y.<!INAPPLICABLE_CANDIDATE!>length<!>
}