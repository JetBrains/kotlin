fun case_18(a: String?, b: Boolean) {
    val x = null
    val y = null

    if (a != (if (b) x else y)) {
        <!DEBUG_INFO_EXPRESSION_TYPE("DeepObject.A.B.C.D.E.F.G.J? & DeepObject.A.B.C.D.E.F.G.J")!>a<!>
    }
}
