//KT-4866 Resolve does not work inside brackets with unresolved reference before

fun test(i: Int, j: Int) {
    <!UNRESOLVED_REFERENCE!>foo<!>[i, j]
}