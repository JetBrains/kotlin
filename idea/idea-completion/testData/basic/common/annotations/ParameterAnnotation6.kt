val v = 1

fun foo(inlineOptions(InlineOp<caret>) a: Int) { }

// INVOCATION_COUNT: 1
// EXIST: InlineOption
// ABSENT: String
// ABSENT: v
