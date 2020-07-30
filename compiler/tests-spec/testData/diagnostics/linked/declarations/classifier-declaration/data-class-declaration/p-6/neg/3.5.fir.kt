// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_PARAMETER
// SKIP_TXT


// TESTCASE NUMBER: 1
data class Case1(val a: Int, val b: String) {
    fun copy(a: Int = this.a, b: String = this.b): Case1 {
        return TODO()
    }
}

// TESTCASE NUMBER: 2
data class Case2(val a: Int, val b: String) {
    @JvmName("boo")
    fun copy(a: Int = this.a, b: String = this.b): Case1 {
        return TODO()
    }
}
