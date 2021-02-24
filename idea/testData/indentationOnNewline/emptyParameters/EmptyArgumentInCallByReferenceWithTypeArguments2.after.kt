fun <T, V> test() {
    test<Int, Long>(
        <caret>
                   )
}

// SET_TRUE: ALIGN_MULTILINE_METHOD_BRACKETS
// IGNORE_FORMATTER
// KT-39459