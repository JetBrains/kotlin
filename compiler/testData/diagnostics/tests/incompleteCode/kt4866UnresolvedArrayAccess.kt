//KT-4866 Resolve does not work inside brackets with unresolved reference before

fun test(i: Int, j: Int) {
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!><!UNRESOLVED_REFERENCE!>foo<!>[i, j]<!>
}