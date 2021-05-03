val v = 1

enum class InlineOption {
    A, B
}

annotation class inlineOptions(val x: InlineOption)

fun foo(@inlineOptions(<caret>) { }

// INVOCATION_COUNT: 1
// EXIST: InlineOption
// EXIST: String
// EXIST: v
// FIR_COMPARISON
