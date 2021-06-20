val v = 1

enum class InlineOption {
    A, B
}

annotation class inlineOptions(val x: InlineOption)

fun foo(@inlineOptions(InlineOp<caret>) a: Int) { }

// INVOCATION_COUNT: 1
// EXIST: InlineOption
// ABSENT: String
// ABSENT: v
