// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION



// TESTCASE NUMBER: 1
data class Case1(val a: Int, val b: CharSequence) {
    fun toString(): String = TODO() //(1)
}

// TESTCASE NUMBER: 2
data class Case2(val a: Int, val b: CharSequence) {
    fun equals(other: Any?): Boolean = TODO() //(0)
}

// TESTCASE NUMBER: 3
data class Case3(val a: Int, val b: CharSequence) {
    fun hashCode(): Int = TODO() //(0)
}
