// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION


// TESTCASE NUMBER: 1
data class Case1(val a: Int, val b: CharSequence) {
    override fun toString(): String = TODO()
}

fun case1(b: Case1) {
    b.<!DEBUG_INFO_CALL("fqName: Case1.toString; typeCall: function")!>toString()<!>
}

// TESTCASE NUMBER: 2
data class Case2(val a: Int, val b: CharSequence) {
    override fun equals(other: Any?): Boolean = TODO()
}

fun case2(b: Case2) {
    b.<!DEBUG_INFO_CALL("fqName: Case2.equals; typeCall: function")!>equals("")<!>
}


// TESTCASE NUMBER: 1
data class Case3(val a: Int, val b: CharSequence) {
    override fun hashCode(): Int = TODO()
}

fun case1(b: Case3) {
    b.<!DEBUG_INFO_CALL("fqName: Case3.hashCode; typeCall: function")!>hashCode()<!>
}
