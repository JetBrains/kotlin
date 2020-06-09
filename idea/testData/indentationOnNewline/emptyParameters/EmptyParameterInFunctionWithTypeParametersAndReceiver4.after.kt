class A {
    fun <T> List<T>?.testFunction(
        <caret>
                                 )
}

// SET_TRUE: ALIGN_MULTILINE_METHOD_BRACKETS
// IGNORE_FORMATTER
// KT-39459