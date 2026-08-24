// WITH_STDLIB

@JvmInline
value class InnerInlineValueClass(val value: Int)

@JvmInline
value class OuterInlineValueClass(val value: InnerInlineValueClass)

fun consume(value: <expr>OuterInlineValueClass</expr>) {}
