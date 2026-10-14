// TARGET_PLATFORM: Common

@JvmInline
value class InlineValueClassCommon(val value: Int)

fun consume(value: <expr>InlineValueClassCommon</expr>) {}
