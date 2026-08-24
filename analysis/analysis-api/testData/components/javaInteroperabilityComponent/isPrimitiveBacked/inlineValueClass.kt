// WITH_STDLIB

@JvmInline
value class InlineValueClass(val value: Int)

fun consume(value: <expr>InlineValueClass</expr>) {}
