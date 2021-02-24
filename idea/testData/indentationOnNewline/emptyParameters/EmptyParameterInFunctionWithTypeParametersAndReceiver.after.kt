class A {
    fun <T, V> Int.Companion.testParam(
        <caret>
    ) {
    }
}

// SET_FALSE: ALIGN_MULTILINE_METHOD_BRACKETS