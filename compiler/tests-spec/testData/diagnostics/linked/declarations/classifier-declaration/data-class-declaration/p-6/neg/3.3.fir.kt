// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNREACHABLE_CODE
// SKIP_TXT

// TESTCASE NUMBER: 1
data class Case1(val a: Int, val b: String) {
    fun component1(): Int {
        return TODO()
    }
}

// TESTCASE NUMBER: 2
data class Case2(val a: Int, val b: String) {
    fun component2(): String {
        return TODO()
    }
}

// TESTCASE NUMBER: 3
data class Case3(val a: Int, val b: String) {
    fun component1(): Int {
        return TODO()
    }

    fun component2(): String {
        return TODO()
    }
}
