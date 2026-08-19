// WITH_STDLIB
// TARGET_PLATFORM: JVM

@JvmInline
value class Inner(val value: String)

@JvmInline
value class Outer(val in<caret>ner: Inner)
