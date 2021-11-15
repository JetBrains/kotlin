// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class InlineFloat(val data: Float)

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class InlineDouble(val data: Double)

fun box(): String {
    if (InlineFloat(0.0f) == InlineFloat(-0.0f)) throw AssertionError()
    if (InlineFloat(Float.NaN) != InlineFloat(Float.NaN)) throw AssertionError()

    if (InlineDouble(0.0) == InlineDouble(-0.0)) throw AssertionError()
    if (InlineDouble(Double.NaN) != InlineDouble(Double.NaN)) throw AssertionError()

    return "OK"
}
