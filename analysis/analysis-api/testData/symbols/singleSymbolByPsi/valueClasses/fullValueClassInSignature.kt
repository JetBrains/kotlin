// LANGUAGE: +FullValueClasses
// TARGET_PLATFORM: JVM

value class FullValueClass(val value: Int)

fun consumeFullValue<caret>Class(value: FullValueClass) {}
