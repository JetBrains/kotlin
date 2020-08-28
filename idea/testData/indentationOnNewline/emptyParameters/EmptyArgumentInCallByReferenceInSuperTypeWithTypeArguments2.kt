open class A<T, C, D<T>>

class B : A<Int, Long, List<Int>>(<caret>)

// SET_TRUE: ALIGN_MULTILINE_METHOD_BRACKETS
// IGNORE_FORMATTER
// KT-39459