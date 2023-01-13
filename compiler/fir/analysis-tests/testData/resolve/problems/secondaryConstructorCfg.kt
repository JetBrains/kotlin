// !DUMP_CFG
class B(p0: String) {
    val p1 = p0
    val p2: Int = p0.length
    var p3: String
    constructor(p0: String, p1: String) : this(p0) {
        p3 = p1
    }
    init {
        <!VAL_REASSIGNMENT!>p1<!> = <!ASSIGNMENT_TYPE_MISMATCH!>p0.length<!>
        p3 = ""
    }
}
