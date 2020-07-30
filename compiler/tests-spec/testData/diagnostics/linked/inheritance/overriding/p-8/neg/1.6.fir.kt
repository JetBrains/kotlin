// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION


// TESTCASE NUMBER: 1
data class Case1(val a: Int, val b: CharSequence) {
    override fun toString(): CharSequence = TODO() //(1)
}

// TESTCASE NUMBER: 2
data class Case2(val a: Int, val b: CharSequence) {
    override fun equals(other: Any?): Any = TODO() //(0)
}

// TESTCASE NUMBER: 3
data class Case3(val a: Int, val b: CharSequence) {
    override fun hashCode(): Any = TODO() //(0)
}
