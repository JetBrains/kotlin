// LANGUAGE: +FullValueClasses
// TARGET_PLATFORM: JVM
// WITH_STDLIB

@JvmInline
value class InlineValueClass(val value: Int)

value class FullValueClass(val first: String, val second: Int)

value object FullValueObject
