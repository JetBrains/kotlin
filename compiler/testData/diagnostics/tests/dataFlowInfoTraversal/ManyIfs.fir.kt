// !CHECK_TYPE

fun noUselessDataFlowInfoCreation(x: Number) {
    if (x is Int) { if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) {
    if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) {
    if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) {
    if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) {
    if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) { if (<!USELESS_IS_CHECK!>x is Int<!>) {
    } } } } } } } } } } } } } } } } } } } } } } } } }
}

fun dataFlowInfoAnd(a: Array<Number>) {
    if (a[0] is Int) { if (a[1] is Int) { if (a[2] is Int) { if (a[3] is Int) { if (a[4] is Int) {
    if (a[5] is Int) { if (a[6] is Int) { if (a[7] is Int) { if (a[8] is Int) { if (a[9] is Int) {
    if (a[10] is Int) { if (a[11] is Int) { if (a[12] is Int) { if (a[13] is Int) { if (a[14] is Int) {
    if (a[15] is Int) { if (a[16] is Int) { if (a[17] is Int) { if (a[18] is Int) { if (a[19] is Int) {
    if (a[20] is Int) { if (a[21] is Int) { if (a[22] is Int) { if (a[23] is Int) { if (a[24] is Int) {
    if (a[25] is Int) { if (a[26] is Int) { if (a[27] is Int) { if (a[28] is Int) { if (a[29] is Int) {
        checkSubtype<Int>(<!ARGUMENT_TYPE_MISMATCH!>a[0]<!>)
    } } } } } } } } } } } } } } } } } } } } } } } } } } } } } }
}

fun dataFlowInfoOr(a: Array<Number>) {
    if (a[0] is Int || a[1] is Int || a[2] is Int || a[3] is Int || a[4] is Int || a[5] is Int || a[6] is Int || a[7] is Int || a[8] is Int || a[9] is Int ||
        a[10] is Int || a[11] is Int || a[12] is Int || a[13] is Int || a[14] is Int || a[15] is Int || a[16] is Int || a[17] is Int || a[18] is Int || a[19] is Int ||
        a[20] is Int || a[21] is Int || a[22] is Int || a[23] is Int || a[24] is Int || a[25] is Int || a[26] is Int || a[27] is Int || a[28] is Int || a[29] is Int ||
        a[30] is Int || a[31] is Int || a[32] is Int || a[33] is Int || a[34] is Int || a[35] is Int || a[36] is Int || a[37] is Int || a[38] is Int || a[39] is Int ||
        a[40] is Int || a[41] is Int || a[42] is Int || a[43] is Int || a[44] is Int || a[45] is Int || a[46] is Int || a[47] is Int || a[48] is Int || a[49] is Int ||
        a[50] is Int || a[51] is Int || a[52] is Int || a[53] is Int || a[54] is Int || a[55] is Int || a[56] is Int || a[57] is Int || a[58] is Int || a[59] is Int) {
        checkSubtype<Int>(<!ARGUMENT_TYPE_MISMATCH!>a[0]<!>)
    }
}
