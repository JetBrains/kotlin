package test

@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
expect inline class ExpectInlineActualInline(val value: Int)

@Suppress("INLINE_CLASS_DEPRECATED", "EXPERIMENTAL_FEATURE_WARNING")
expect inline class ExpectInlineActualValue(val value: Int)

actual typealias ExpectInlineActualInline = lib.InlineClass
actual typealias ExpectInlineActualValue = lib.ValueClass
