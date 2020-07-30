// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNREACHABLE_CODE
// SKIP_TXT

// TESTCASE NUMBER: 1
data class Case1(val a: Int, val b: String) {
    @JvmName("boo")
    operator fun component1(): Int {
        return TODO()
    }
}

// TESTCASE NUMBER: 2
data class Case2(val a: Int, val b: String) {
    @JvmName("boo")
    operator fun component2(): String {
        return TODO()
    }
}

// TESTCASE NUMBER: 3
data class Case3(val a: Int, val b: String) {
    @JvmName("boo1")
    operator fun component1(): Int {
        return TODO()
    }

    @JvmName("boo2")
    operator fun component2(): String {
        return TODO()
    }
}
