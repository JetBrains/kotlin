open class A

class B : A(
    <caret>
           )

// SET_TRUE: ALIGN_MULTILINE_METHOD_BRACKETS
// IGNORE_FORMATTER
// KT-39459